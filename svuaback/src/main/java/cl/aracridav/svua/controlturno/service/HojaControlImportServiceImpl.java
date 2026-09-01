package cl.aracridav.svua.controlturno.service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import cl.aracridav.svua.controlturno.dto.response.ImportHojaControlResponse;
import cl.aracridav.svua.controlturno.entity.LecturaControl;
import cl.aracridav.svua.controlturno.entity.PuntoControl;
import cl.aracridav.svua.controlturno.enums.TurnoTrabajo;
import cl.aracridav.svua.controlturno.repository.LecturaControlRepository;
import cl.aracridav.svua.controlturno.repository.PuntoControlRepository;
import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.empresa.repository.EmpresaRepository;
import cl.aracridav.svua.shared.exception.BusinessException;
import cl.aracridav.svua.shared.util.SecurityUtils;
import cl.aracridav.svua.usuario.entity.Usuario;
import cl.aracridav.svua.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

// 🔥 Importa la planilla real "SISTEMA_DE_CONTROL_DE_MANTENCION.xlsx",
// hoja "HOJA DE CONTROL" (la que este modulo reemplaza, ver
// V29/V30__...sql), creando las lecturas de HOY para cada punto de
// control y hora que trae el archivo. A diferencia del importador
// generico de Activos/Proveedores/etc (ExcelImportServiceImpl), esta
// planilla no tiene una fila por registro: es una plantilla de layout
// FIJO (siempre la misma copia diaria), con bloques de celdas en
// posiciones conocidas -- por eso el mapeo es por coordenada de
// fila/columna, no por encabezado de columna (los encabezados de la
// planilla real traen errores de tipeo inconsistentes segun el dia).
@Service
@RequiredArgsConstructor
@Transactional
public class HojaControlImportServiceImpl implements HojaControlImportService {

    private static final String NOMBRE_HOJA = "HOJA DE CONTROL";

    // La planilla registra lecturas horarias de 02:00 a 19:00 (18 horas).
    private static final int PRIMERA_HORA = 2;
    private static final int CANTIDAD_HORAS = 18;

    // Los bloques "horizontales" (fermentacion, velocidad) traen las 18
    // horas en columnas B..S, siempre a partir de esta columna
    // (0-indexado: A=0, B=1).
    private static final int PRIMERA_COLUMNA_HORIZONTAL = 1;

    // 🔒 Horario de turno (Chile, planta): confirmado con el usuario --
    // MAÑANA 06-14, TARDE 14-22, NOCHE 22-06. No hay un valor "oficial"
    // en el resto del sistema (el formulario manual siempre pide elegir
    // el turno a mano), asi que si el horario de planta cambia hay que
    // actualizar estos 2 cortes.
    private static final int INICIO_MANANA = 6;
    private static final int INICIO_TARDE = 14;
    private static final int INICIO_NOCHE = 22;

    // Unidad de los puntos que se crean al vuelo si no existen en el
    // catalogo (Sala Masas, Sala Rondo, Temp Exterior Ambiente): las 3
    // son de temperatura en la planilla original ("T° SALA...").
    private static final String UNIDAD_TEMPERATURA = "°C";

    private final LecturaControlRepository lecturaRepository;
    private final PuntoControlRepository puntoControlRepository;
    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;

    // Una columna de un bloque "vertical": a que punto de control
    // corresponde, y si su valor viene como fraccion (0.77) y hay que
    // llevarlo a porcentaje (77) -- asi vienen las columnas de Humedad
    // en la planilla original (celda con formato de porcentaje de Excel).
    private record PuntoColumna(String nombrePunto, boolean convertirAPorcentaje) {}

    // 🔒 `filaInicio`/`filaValor` son índices de FILA DE POI (0-indexado:
    // fila 0 = primera fila de la hoja), NO el número de fila que se ve
    // en Excel (1-indexado). Por eso cada constante de abajo es el
    // número de fila de Excel MENOS 1 -- se deja comentado el número de
    // Excel real al lado para poder verificarlo contra el archivo.
    private record BloqueVertical(int filaInicio, Map<Integer, PuntoColumna> columnaPunto) {}

    private record BloqueHorizontal(int filaValor, String nombrePunto) {}

    private static final List<BloqueVertical> BLOQUES_VERTICALES = List.of(
        new BloqueVertical(13, mapaProofer("Proofer 1")),  // Excel fila 14
        new BloqueVertical(37, mapaProofer("Proofer 2")),  // Excel fila 38
        new BloqueVertical(59, mapaCamarasYSalas())        // Excel fila 60
    );

    private static final List<BloqueHorizontal> BLOQUES_HORIZONTALES = List.of(
        new BloqueHorizontal(80, "Tiempo de Fermentación Proofer N°1"),               // Excel fila 81
        new BloqueHorizontal(83, "Tiempo de Fermentación Proofer N°2"),               // Excel fila 84
        new BloqueHorizontal(86, "Velocidad Espirales de Enfriado y Congelado"),      // Excel fila 87
        new BloqueHorizontal(89, "Velocidad Freidora N°1"),                          // Excel fila 90
        new BloqueHorizontal(92, "Velocidad Freidora N°2")                          // Excel fila 93
    );

    private static final Set<String> PUNTOS_NUEVOS_A_CREAR = Set.of(
        "Sala Masas", "Sala Rondo", "Temp Exterior Ambiente"
    );

    // Columnas 0-indexadas (A=0, B=1, C=2...): temperatura en C,D,E,F
    // (secciones 1-4) y humedad en H,I,J,K (secciones 1-4), para cada
    // Proofer.
    private static Map<Integer, PuntoColumna> mapaProofer(String prefijo) {
        Map<Integer, PuntoColumna> m = new LinkedHashMap<>();
        m.put(2, new PuntoColumna(prefijo + " - Temperatura Sección N°1", false));
        m.put(3, new PuntoColumna(prefijo + " - Temperatura Sección N°2", false));
        m.put(4, new PuntoColumna(prefijo + " - Temperatura Sección N°3", false));
        m.put(5, new PuntoColumna(prefijo + " - Temperatura Sección N°4", false));
        m.put(7, new PuntoColumna(prefijo + " - Humedad Sección N°1", true));
        m.put(8, new PuntoColumna(prefijo + " - Humedad Sección N°2", true));
        m.put(9, new PuntoColumna(prefijo + " - Humedad Sección N°3", true));
        m.put(10, new PuntoColumna(prefijo + " - Humedad Sección N°4", true));
        return m;
    }

    // Columnas: B=Cámara Variedades 1, C=Cámara Variedades 2,
    // D=Cámara de Congelado, G=Sala de Envasado, H=Sala de Variedades,
    // I=Sala Masas, J=Sala Rondo, K=Temp Exterior Ambiente.
    private static Map<Integer, PuntoColumna> mapaCamarasYSalas() {
        Map<Integer, PuntoColumna> m = new LinkedHashMap<>();
        m.put(1, new PuntoColumna("Cámara Variedades 1", false));
        m.put(2, new PuntoColumna("Cámara Variedades 2", false));
        m.put(3, new PuntoColumna("Cámara de Congelado", false));
        m.put(6, new PuntoColumna("Sala de Envasado", false));
        m.put(7, new PuntoColumna("Sala de Variedades", false));
        m.put(8, new PuntoColumna("Sala Masas", false));
        m.put(9, new PuntoColumna("Sala Rondo", false));
        m.put(10, new PuntoColumna("Temp Exterior Ambiente", false));
        return m;
    }

    @Override
    public ImportHojaControlResponse importar(MultipartFile archivo) {

        validarControlTurnoHabilitado();
        validarHojaControlHabilitado();

        Long empresaId = SecurityUtils.getEmpresaId();

        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new BusinessException("Empresa no encontrada"));

        Usuario usuario = usuarioRepository.findById(SecurityUtils.getUsuarioId())
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));

        try (InputStream is = archivo.getInputStream();
                Workbook workbook = new XSSFWorkbook(is)) {

            Sheet hoja = workbook.getSheet(NOMBRE_HOJA);

            if (hoja == null) {
                throw new BusinessException(
                        "El archivo no tiene una hoja llamada \"" + NOMBRE_HOJA + "\"");
            }

            LocalDate hoy = LocalDate.now();

            int[] contadores = new int[2]; // [0]=creadas, [1]=omitidas
            List<String> puntosNuevos = new ArrayList<>();

            for (BloqueVertical bloque : BLOQUES_VERTICALES) {
                importarBloqueVertical(hoja, bloque, empresa, usuario, hoy, contadores, puntosNuevos);
            }

            for (BloqueHorizontal bloque : BLOQUES_HORIZONTALES) {
                importarBloqueHorizontal(hoja, bloque, empresa, usuario, hoy, contadores, puntosNuevos);
            }

            return ImportHojaControlResponse.builder()
                    .lecturasCreadas(contadores[0])
                    .lecturasOmitidas(contadores[1])
                    .puntosNuevosCreados(puntosNuevos.stream().distinct().toList())
                    .build();

        } catch (IOException e) {
            throw new BusinessException("No fue posible leer el archivo Excel");
        }
    }

    private void importarBloqueVertical(
            Sheet hoja, BloqueVertical bloque, Empresa empresa, Usuario usuario,
            LocalDate hoy, int[] contadores, List<String> puntosNuevos) {

        for (int i = 0; i < CANTIDAD_HORAS; i++) {

            Row row = hoja.getRow(bloque.filaInicio() + i);

            if (row == null) {
                continue;
            }

            int hora = PRIMERA_HORA + i;

            for (Map.Entry<Integer, PuntoColumna> entry : bloque.columnaPunto().entrySet()) {

                PuntoColumna puntoColumna = entry.getValue();

                BigDecimal valor = leerValorNumerico(row, entry.getKey());

                if (valor == null) {
                    continue;
                }

                if (puntoColumna.convertirAPorcentaje()) {
                    valor = valor.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
                }

                registrarLectura(
                        empresa, usuario, puntoColumna.nombrePunto(), valor, hoy, hora,
                        contadores, puntosNuevos);
            }
        }
    }

    private void importarBloqueHorizontal(
            Sheet hoja, BloqueHorizontal bloque, Empresa empresa, Usuario usuario,
            LocalDate hoy, int[] contadores, List<String> puntosNuevos) {

        Row row = hoja.getRow(bloque.filaValor());

        if (row == null) {
            return;
        }

        for (int i = 0; i < CANTIDAD_HORAS; i++) {

            int hora = PRIMERA_HORA + i;

            BigDecimal valor = leerValorNumerico(row, PRIMERA_COLUMNA_HORIZONTAL + i);

            if (valor == null) {
                continue;
            }

            registrarLectura(
                    empresa, usuario, bloque.nombrePunto(), valor, hoy, hora,
                    contadores, puntosNuevos);
        }
    }

    private void registrarLectura(
            Empresa empresa, Usuario usuario, String nombrePunto, BigDecimal valor,
            LocalDate fecha, int hora, int[] contadores, List<String> puntosNuevos) {

        PuntoControl punto = obtenerOCrearPunto(empresa, nombrePunto, puntosNuevos);

        LocalDateTime fechaHora = fecha.atTime(hora, 0);

        if (lecturaRepository.existsByPuntoControlIdAndFechaHora(punto.getId(), fechaHora)) {
            contadores[1]++;
            return;
        }

        LecturaControl lectura = new LecturaControl();
        lectura.setPuntoControl(punto);
        lectura.setValor(valor);
        lectura.setFechaHora(fechaHora);
        lectura.setTurno(turnoParaHora(hora));
        lectura.setObservacion("Cargado desde Excel (" + NOMBRE_HOJA + ")");
        lectura.setUsuario(usuario);
        lectura.setEmpresa(empresa);

        lecturaRepository.save(lectura);
        contadores[0]++;
    }

    // 🔒 "Buscar o crear": los 26 puntos ya existentes se reutilizan tal
    // cual; los 3 que la planilla trae pero el catalogo no tiene aun
    // (PUNTOS_NUEVOS_A_CREAR) se crean la primera vez que se importa un
    // archivo, y de ahi en adelante ya quedan en el catalogo normal.
    private PuntoControl obtenerOCrearPunto(Empresa empresa, String nombre, List<String> puntosNuevos) {

        return puntoControlRepository.findByNombreIgnoreCaseAndEmpresaId(nombre, empresa.getId())
                .orElseGet(() -> {

                    PuntoControl nuevo = new PuntoControl();
                    nuevo.setNombre(nombre);
                    nuevo.setUnidad(UNIDAD_TEMPERATURA);
                    nuevo.setActivo(true);
                    nuevo.setEmpresa(empresa);

                    PuntoControl guardado = puntoControlRepository.save(nuevo);

                    if (PUNTOS_NUEVOS_A_CREAR.contains(nombre)) {
                        puntosNuevos.add(nombre);
                    }

                    return guardado;
                });
    }

    private BigDecimal leerValorNumerico(Row row, int columna) {

        Cell cell = row.getCell(columna);

        if (cell == null || cell.getCellType() != CellType.NUMERIC) {
            return null;
        }

        return BigDecimal.valueOf(cell.getNumericCellValue()).setScale(2, RoundingMode.HALF_UP);
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

    private void validarControlTurnoHabilitado() {
        if (!SecurityUtils.esSuperAdmin() && !SecurityUtils.tieneControlTurnoHabilitado()) {
            throw new BusinessException("Control de Turno no está habilitado para su empresa");
        }
    }

    // 🔒 Defensa en profundidad: el frontend ya oculta el botón "Importar
    // Excel" si la empresa no tiene este flag, pero eso no impide una
    // llamada directa a la API.
    private void validarHojaControlHabilitado() {
        if (!SecurityUtils.esSuperAdmin() && !SecurityUtils.tieneHojaControlHabilitado()) {
            throw new BusinessException("La importación desde Excel no está habilitada para su empresa");
        }
    }
}
