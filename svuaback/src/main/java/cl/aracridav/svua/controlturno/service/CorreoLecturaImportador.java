package cl.aracridav.svua.controlturno.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import cl.aracridav.svua.controlturno.entity.DispositivoEmpresa;
import cl.aracridav.svua.controlturno.entity.LecturaControl;
import cl.aracridav.svua.controlturno.entity.PuntoControl;
import cl.aracridav.svua.controlturno.enums.TurnoTrabajo;
import cl.aracridav.svua.controlturno.repository.DispositivoEmpresaRepository;
import cl.aracridav.svua.controlturno.repository.LecturaControlRepository;
import cl.aracridav.svua.controlturno.repository.PuntoControlRepository;
import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.multitenancy.RlsContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 🔥 Procesa UNA hoja del Excel adjunto -- ver CorreoLecturaImportService,
// que abre el workbook y llama procesarHoja() por cada hoja que trae.
//
// 🔒 Cada hoja puede ser de una empresa DISTINTA (el correo lo manda un
// proveedor externo para todas las empresas por igual, ver
// DispositivoEmpresa): por eso la empresa no se sabe de antemano, se
// descubre aca mismo a partir del "Dispositivo: XXX" que trae la hoja,
// y por eso procesarHoja() tiene su PROPIA transaccion independiente
// (REQUIRES_NEW) -- si una hoja de la empresa A falla o no tiene
// dispositivo registrado, no debe afectar a otra hoja de la empresa B
// en el mismo archivo, y las lecturas que sí se guardaron quedan
// confirmadas aunque algo posterior (ej. mover el correo) falle
// despues.
@Service
@RequiredArgsConstructor
@Slf4j
public class CorreoLecturaImportador {

    private static final String TEMPERATURA = "TEMPERATURA";
    private static final String HUMEDAD = "HUMEDAD";
    private static final int COLUMNA_TEMPERATURA = 1; // columna B (0-indexado)
    private static final int COLUMNA_HUMEDAD = 2;      // columna C (0-indexado)
    private static final int FILAS_MAXIMAS_PARA_ENCABEZADO = 20;

    // Ej. "Dispositivo: INS-877  |  Modelo: RCW-3200 HF TH" -> "INS-877"
    private static final Pattern PATRON_DISPOSITIVO = Pattern.compile("(?i)dispositivo\\s*:\\s*([^|]+)");

    // 🔒 Mismo horario de turno usado en HojaControlImportServiceImpl
    // (confirmado con el usuario: MAÑANA 06-14, TARDE 14-22, NOCHE 22-06).
    private static final int INICIO_MANANA = 6;
    private static final int INICIO_TARDE = 14;
    private static final int INICIO_NOCHE = 22;

    private final LecturaControlRepository lecturaRepository;
    private final PuntoControlRepository puntoControlRepository;
    private final DispositivoEmpresaRepository dispositivoEmpresaRepository;
    private final RlsContextService rlsContextService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void procesarHoja(Sheet hoja) {

        // 🔒 todavia no se sabe la empresa (es lo que este paso busca
        // averiguar): bypass para poder buscar el dispositivo cruzando
        // todas las empresas.
        rlsContextService.aplicarBypass();

        try {
            String codigoDispositivo = extraerCodigoDispositivo(hoja);

            if (codigoDispositivo == null) {
                log.warn("Correo Control de Turno: hoja '{}' no trae un 'Dispositivo: ...' identificable, se omite",
                        hoja.getSheetName());
                return;
            }

            DispositivoEmpresa dispositivo = dispositivoEmpresaRepository
                    .findByCodigoDispositivoIgnoreCaseAndActivoTrue(codigoDispositivo)
                    .orElse(null);

            if (dispositivo == null) {
                log.warn("Correo Control de Turno: el dispositivo '{}' no esta registrado a ninguna empresa "
                        + "(ver catalogo Dispositivos), se omite la hoja '{}'", codigoDispositivo, hoja.getSheetName());
                return;
            }

            Empresa empresa = dispositivo.getEmpresa();

            // 🔒 recien aca se sabe la empresa: se acota el contexto RLS
            // a partir de este punto, para los guardados de mas abajo.
            rlsContextService.aplicarEmpresa(empresa.getId());

            procesarDatosHoja(hoja, empresa);

        } finally {
            rlsContextService.aplicarBypass();
        }
    }

    private void procesarDatosHoja(Sheet hoja, Empresa empresa) {

        String tipoMetrica = detectarTipoMetrica(hoja.getSheetName());
        if (tipoMetrica == null) {
            log.warn("Correo Control de Turno: hoja '{}' no indica si es temperatura o humedad, se omite",
                    hoja.getSheetName());
            return;
        }

        int columnaValor = TEMPERATURA.equals(tipoMetrica) ? COLUMNA_TEMPERATURA : COLUMNA_HUMEDAD;
        String unidad = TEMPERATURA.equals(tipoMetrica) ? "°C" : "%";

        int filaEncabezado = buscarFilaEncabezado(hoja);
        if (filaEncabezado < 0) {
            log.warn("Correo Control de Turno: hoja '{}' no tiene una fila de encabezado 'Fecha/Hora', se omite",
                    hoja.getSheetName());
            return;
        }

        String nombrePunto = normalizarNombrePunto(hoja.getSheetName());
        PuntoControl punto = obtenerOCrearPunto(empresa, nombrePunto, unidad);

        int creadas = 0;
        int omitidas = 0;

        for (int i = filaEncabezado + 1; i <= hoja.getLastRowNum(); i++) {

            Row fila = hoja.getRow(i);
            if (fila == null) {
                continue;
            }

            LocalDateTime fechaHora = leerFechaHora(fila.getCell(0));
            if (fechaHora == null) {
                // fin del bloque de datos (bloque de "Estadisticas" o fila vacia)
                break;
            }

            BigDecimal valor = leerValorNumerico(fila, columnaValor);
            if (valor == null) {
                continue;
            }

            if (registrarLectura(empresa, punto, valor, fechaHora)) {
                creadas++;
            } else {
                omitidas++;
            }
        }

        log.info("Correo Control de Turno: hoja '{}' (empresa '{}') -> punto '{}' "
                + "({} lecturas creadas, {} omitidas por duplicadas)",
                hoja.getSheetName(), empresa.getNombre(), punto.getNombre(), creadas, omitidas);
    }

    private String extraerCodigoDispositivo(Sheet hoja) {

        int limite = Math.min(hoja.getLastRowNum(), FILAS_MAXIMAS_PARA_ENCABEZADO);

        for (int i = 0; i <= limite; i++) {

            Row fila = hoja.getRow(i);
            if (fila == null) {
                continue;
            }

            Cell celda = fila.getCell(0);
            if (celda == null || celda.getCellType() != CellType.STRING) {
                continue;
            }

            Matcher matcher = PATRON_DISPOSITIVO.matcher(celda.getStringCellValue());
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }

        return null;
    }

    private String detectarTipoMetrica(String nombreHoja) {
        String enMinuscula = nombreHoja.toLowerCase();
        if (enMinuscula.contains("temperatura") || enMinuscula.contains("temperature")) {
            return TEMPERATURA;
        }
        if (enMinuscula.contains("humedad") || enMinuscula.contains("humidity")) {
            return HUMEDAD;
        }
        return null;
    }

    private int buscarFilaEncabezado(Sheet hoja) {

        int limite = Math.min(hoja.getLastRowNum(), FILAS_MAXIMAS_PARA_ENCABEZADO);

        for (int i = 0; i <= limite; i++) {

            Row fila = hoja.getRow(i);
            if (fila == null) {
                continue;
            }

            Cell celda = fila.getCell(0);
            if (celda != null && celda.getCellType() == CellType.STRING
                    && "fecha/hora".equalsIgnoreCase(celda.getStringCellValue().trim())) {
                return i;
            }
        }

        return -1;
    }

    // 🐛 El catalogo de puntos usa "Sección N°1" (con espacios alrededor
    // del guion y "N°"), pero el reporte puede traer la hoja nombrada
    // "Sección 1" o "1-Temperatura" (sin espacios, sin "N°"). Se
    // normaliza para intentar calzar con el punto ya existente en vez de
    // crear uno nuevo casi-duplicado.
    private String normalizarNombrePunto(String nombreHoja) {
        String normalizado = nombreHoja.trim();
        normalizado = normalizado.replaceAll("\\s*-\\s*", " - ");
        normalizado = normalizado.replaceAll("(?i)(secci[oó]n)\\s+(?!N°)(\\d+)", "$1 N°$2");
        return normalizado;
    }

    private PuntoControl obtenerOCrearPunto(Empresa empresa, String nombre, String unidad) {
        return puntoControlRepository.findByNombreIgnoreCaseAndEmpresaId(nombre, empresa.getId())
                .orElseGet(() -> {
                    PuntoControl nuevo = new PuntoControl();
                    nuevo.setNombre(nombre);
                    nuevo.setUnidad(unidad);
                    nuevo.setActivo(true);
                    nuevo.setEmpresa(empresa);
                    return puntoControlRepository.save(nuevo);
                });
    }

    private boolean registrarLectura(Empresa empresa, PuntoControl punto, BigDecimal valor, LocalDateTime fechaHora) {

        if (lecturaRepository.existsByPuntoControlIdAndFechaHora(punto.getId(), fechaHora)) {
            return false;
        }

        LecturaControl lectura = new LecturaControl();
        lectura.setPuntoControl(punto);
        lectura.setValor(valor);
        lectura.setFechaHora(fechaHora);
        lectura.setTurno(turnoParaHora(fechaHora.getHour()));
        lectura.setObservacion("Cargado automáticamente desde correo");
        lectura.setEmpresa(empresa);

        lecturaRepository.save(lectura);
        return true;
    }

    private LocalDateTime leerFechaHora(Cell celda) {
        if (celda == null || celda.getCellType() != CellType.NUMERIC || !DateUtil.isCellDateFormatted(celda)) {
            return null;
        }
        return celda.getLocalDateTimeCellValue();
    }

    private BigDecimal leerValorNumerico(Row fila, int columna) {

        Cell celda = fila.getCell(columna);

        if (celda == null || celda.getCellType() != CellType.NUMERIC) {
            return null;
        }

        return BigDecimal.valueOf(celda.getNumericCellValue()).setScale(2, RoundingMode.HALF_UP);
    }

    private TurnoTrabajo turnoParaHora(int hora) {

        if (hora >= INICIO_MANANA && hora < INICIO_TARDE) {
            return TurnoTrabajo.MANANA;
        }

        if (hora >= INICIO_TARDE && hora < INICIO_NOCHE) {
            return TurnoTrabajo.TARDE;
        }

        return TurnoTrabajo.NOCHE;
    }
}
