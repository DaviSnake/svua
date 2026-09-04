package cl.aracridav.svua.controlturno.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import cl.aracridav.svua.shared.exception.BusinessException;
import jakarta.mail.BodyPart;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.Store;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 🔥 Revisa la bandeja de entrada de soporte@nexovectoria.cl por IMAP
// (ver CorreoLecturaImportScheduler, cada 10 min), busca correos con un
// adjunto .xlsx (reportes de dispositivos de medicion enviados por
// correo), y delega cada HOJA del archivo a
// CorreoLecturaImportador.procesarHoja() -- esta clase NO sabe a que
// empresa pertenece nada de esto: el correo lo manda un proveedor
// externo para todas las empresas por igual, asi que la empresa se
// descubre hoja por hoja (por el dispositivo que trae cada una, ver esa
// clase), no de antemano. Cada correo procesado (tenga o no exito la
// importacion de su contenido) se mueve a la carpeta IMAP "Procesados"
// para no reprocesarlo en el proximo ciclo -- la bandeja de entrada es
// la unica fuente que este job mira.
//
// Formato del archivo (confirmado con un reporte real de ejemplo de un
// dispositivo de medicion): una hoja por punto de control. Las
// primeras filas son info del dispositivo/periodo (variable en
// cantidad), la fila de encabezado real dice "Fecha/Hora" en la
// columna A (se busca dinamicamente, no por numero de fila fijo -- a
// diferencia de HojaControlImportServiceImpl, aca el bloque de datos
// no tiene un layout 100% fijo), seguida de filas con fecha/hora,
// temperatura y humedad; termina en un bloque de "Estadisticas"
// (Minimo/Maximo/Promedio) que se detecta porque la columna de
// fecha deja de traer una fecha valida.
@Service
@RequiredArgsConstructor
@Slf4j
public class CorreoLecturaImportService {

    private final CorreoLecturaImportador lecturaImportador;

    @Value("${mail.imap.host}")
    private String imapHost;

    @Value("${mail.imap.port}")
    private int imapPort;

    @Value("${mail.imap.carpeta-procesados}")
    private String carpetaProcesados;

    // 🔒 Propios de esta cuenta IMAP -- no necesariamente los mismos que
    // envian las notificaciones por SMTP (ver application.properties).
    @Value("${mail.imap.username}")
    private String mailUsername;

    @Value("${mail.imap.password}")
    private String mailPassword;

    public void revisarBandejaEImportar() {

        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.ssl.enable", "true");
        // 🔒 Mismo ajuste que ya necesita el SMTP de este servidor (ver
        // spring.mail.properties.mail.smtp.ssl.trust en
        // application.properties): sin esto el handshake TLS puede
        // fallar contra el certificado de mail.nexovectoria.cl.
        props.put("mail.imaps.ssl.trust", imapHost);

        Session session = Session.getInstance(props);

        try (Store store = session.getStore("imaps")) {

            store.connect(imapHost, imapPort, mailUsername, mailPassword);

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);

            try {
                // 🐛 FIX: algunos servidores IMAP (este, cPanel/Dovecot,
                // incluido) exigen que las carpetas nuevas queden
                // anidadas bajo INBOX, con el separador de jerarquia
                // PROPIO de ese servidor (no siempre "." -- puede ser
                // "/"). Crear "Procesados" a secas tira "nonexistent
                // namespace". Se le pregunta el separador real al
                // servidor en vez de asumirlo.
                char separador = inbox.getSeparator();
                String rutaProcesados = "INBOX" + separador + carpetaProcesados;

                procesarBandeja(inbox, obtenerOCrearCarpeta(store, rutaProcesados));
            } finally {
                inbox.close(true);
            }

        } catch (MessagingException ex) {
            throw new BusinessException(
                    "No fue posible conectarse al correo para importar lecturas de Control de Turno", ex);
        }
    }

    private void procesarBandeja(Folder inbox, Folder procesados) throws MessagingException {

        Message[] mensajes = inbox.getMessages();
        List<Message> aMover = new ArrayList<>();

        for (Message mensaje : mensajes) {

            List<byte[]> adjuntos;
            try {
                adjuntos = extraerAdjuntosXlsx(mensaje);
            } catch (Exception ex) {
                log.error("No fue posible leer un correo de la bandeja, se deja para el proximo ciclo: {}",
                        ex.getMessage(), ex);
                continue;
            }

            if (adjuntos.isEmpty()) {
                continue;
            }

            for (byte[] adjunto : adjuntos) {
                try {
                    procesarExcel(adjunto);
                } catch (Exception ex) {
                    log.error("No fue posible procesar un adjunto Excel del correo: {}", ex.getMessage(), ex);
                }
            }

            // 🔒 se mueve igual, haya tenido exito o no el procesamiento
            // del adjunto (pedido explicito): un correo con formato
            // invalido no debe reintentarse indefinidamente cada 10 min.
            aMover.add(mensaje);
        }

        if (aMover.isEmpty()) {
            return;
        }

        Message[] arreglo = aMover.toArray(new Message[0]);
        inbox.copyMessages(arreglo, procesados);
        for (Message mensaje : arreglo) {
            mensaje.setFlag(Flags.Flag.DELETED, true);
        }

        log.info("Correo Control de Turno: {} correo(s) procesado(s) y movido(s) a '{}'",
                arreglo.length, procesados.getName());
    }

    private void procesarExcel(byte[] archivo) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(archivo))) {
            for (Sheet hoja : workbook) {
                try {
                    // 🔒 transaccion propia (REQUIRES_NEW, ver esa clase):
                    // confirma en la BD aunque otra hoja del mismo
                    // archivo (de otra empresa) falle, o aunque el correo
                    // termine sin poder moverse.
                    lecturaImportador.procesarHoja(hoja);
                } catch (Exception ex) {
                    log.error("No fue posible procesar la hoja '{}': {}", hoja.getSheetName(), ex.getMessage(), ex);
                }
            }
        }
    }

    private Folder obtenerOCrearCarpeta(Store store, String nombre) throws MessagingException {
        Folder carpeta = store.getFolder(nombre);
        if (!carpeta.exists()) {
            carpeta.create(Folder.HOLDS_MESSAGES);
        }
        return carpeta;
    }

    private List<byte[]> extraerAdjuntosXlsx(Message mensaje) throws Exception {
        List<byte[]> adjuntos = new ArrayList<>();
        Object contenido = mensaje.getContent();
        if (contenido instanceof Multipart multipart) {
            recolectarAdjuntosXlsx(multipart, adjuntos);
        }
        return adjuntos;
    }

    private void recolectarAdjuntosXlsx(Multipart multipart, List<byte[]> adjuntos) throws Exception {

        for (int i = 0; i < multipart.getCount(); i++) {

            BodyPart parte = multipart.getBodyPart(i);
            String nombreArchivo = parte.getFileName();

            if (nombreArchivo != null && nombreArchivo.toLowerCase().endsWith(".xlsx")) {
                try (InputStream is = parte.getInputStream()) {
                    adjuntos.add(is.readAllBytes());
                }
                continue;
            }

            Object contenidoParte = parte.getContent();
            if (contenidoParte instanceof Multipart anidado) {
                recolectarAdjuntosXlsx(anidado, adjuntos);
            }
        }
    }
}
