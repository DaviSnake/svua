package cl.aracridav.svua.shared.service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import cl.aracridav.svua.depreciacion.service.DepreciacionService;
import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.empresa.repository.EmpresaRepository;
import cl.aracridav.svua.inventario.activo.dto.request.ActivoImportRowDTO;
import cl.aracridav.svua.inventario.activo.dto.response.ActivoImportResultDTO;
import cl.aracridav.svua.inventario.activo.dto.response.ActivoImportRowResultDTO;
import cl.aracridav.svua.inventario.activo.entity.Activo;
import cl.aracridav.svua.inventario.activo.repository.ActivoRepository;
import cl.aracridav.svua.inventario.historial.service.HistorialEstadoActivoService;
import cl.aracridav.svua.inventario.tipoactivo.dto.request.TipoActivoImportRowDTO;
import cl.aracridav.svua.inventario.tipoactivo.entity.TipoActivo;
import cl.aracridav.svua.inventario.tipoactivo.repository.TipoActivoRepository;
import cl.aracridav.svua.inventario.ubicacion.dto.request.UbicacionImportRowDTO;
import cl.aracridav.svua.inventario.ubicacion.entity.Ubicacion;
import cl.aracridav.svua.inventario.ubicacion.repository.UbicacionRepository;
import cl.aracridav.svua.mantenimiento.orden.dto.request.OrdenImportRowDTO;
import cl.aracridav.svua.mantenimiento.orden.entity.EstadoOrden;
import cl.aracridav.svua.mantenimiento.orden.entity.OrdenMantenimiento;
import cl.aracridav.svua.mantenimiento.orden.entity.TipoMantenimiento;
import cl.aracridav.svua.mantenimiento.orden.repository.OrdenMantenimientoRepository;
import cl.aracridav.svua.mantenimiento.repuesto.dto.request.RepuestoImportRowDTO;
import cl.aracridav.svua.mantenimiento.repuesto.entity.Repuesto;
import cl.aracridav.svua.mantenimiento.repuesto.entity.TipoRepuesto;
import cl.aracridav.svua.mantenimiento.repuesto.repository.RepuestoRepository;
import cl.aracridav.svua.proveedor.dto.request.ProveedorImportRowDTO;
import cl.aracridav.svua.proveedor.entity.Proveedor;
import cl.aracridav.svua.proveedor.entity.TipoProveedor;
import cl.aracridav.svua.proveedor.repository.ProveedorRepository;
import cl.aracridav.svua.shared.dto.response.ImportBatchResultDTO;
import cl.aracridav.svua.shared.dto.response.ImportProgressDTO;
import cl.aracridav.svua.shared.dto.response.ImportRowResultDTO;
import cl.aracridav.svua.shared.enums.EstadoActivo;
import cl.aracridav.svua.shared.exception.BusinessException;
import cl.aracridav.svua.shared.util.RutUtils;
import cl.aracridav.svua.usuario.entity.Usuario;
import cl.aracridav.svua.usuario.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@EnableAsync
public class ExcelImportServiceImpl implements ExcelImportService{

    private final EmpresaRepository empresaRepository;
    private final OrdenMantenimientoRepository oMantenimientoRepository;
    private final RepuestoRepository repuestoRepository;
    private final ActivoRepository activoRepository;
    private final DepreciacionService depreciacionService;
    private final HistorialEstadoActivoService historialEstadoActivoService;
    private final UsuarioRepository usuarioRepository;
    private final TipoActivoRepository tipoActivoRepository;
    private final UbicacionRepository ubicacionRepository;
    private final ProveedorRepository proveedorRepository;
    private final ImportProgressService progressService;

    @PersistenceContext
    private EntityManager em;

    private static final int BATCH_SIZE = 100;

    @Override
    @Async
    @Transactional
    public void procesarAsync(Path path, String jobId, Long empresaId, Long usuarioId, String archivo) {

        boolean huboErrores = false;

        try (InputStream is = Files.newInputStream(path);
            Workbook workbook = new XSSFWorkbook(is)) {

            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter();

            Sheet sheet = workbook.getSheetAt(0);
            int total = sheet.getLastRowNum();

            progressService.iniciar(jobId, total);

            List<Activo> batchActivo = new ArrayList<>();
            List<Proveedor> batchProveedor = new ArrayList<>();
            List<OrdenMantenimiento> batchOrden = new ArrayList<>();
            List<Repuesto> batchRepuesto = new ArrayList<>();
            List<Ubicacion> batchUbicacion = new ArrayList<>();
            List<TipoActivo> batchTipoActivo = new ArrayList<>();

            System.out.println("Total Filas: " + total);

            for (Row row : sheet) {

                if (row.getRowNum() == 0) continue;

                if (filaVacia(row)) {
                    total--;
                    continue;
                }

                try {
                    switch (archivo) {

                        case "activo" -> {
                            Activo activo = mapActivo(row, empresaId);
                            batchActivo.add(activo);

                            if (batchActivo.size() == BATCH_SIZE) {
                                guardarActivo(batchActivo, empresaId, usuarioId);
                                batchActivo.clear();
                            }
                        }

                        case "proveedor" -> {
                            Proveedor proveedor = mapProveedor(row, empresaId);
                            batchProveedor.add(proveedor);

                            if (batchProveedor.size() == BATCH_SIZE) {
                                guardarProveedor(batchProveedor);
                                batchProveedor.clear();
                            }
                        }

                        case "orden" -> {
                            OrdenMantenimiento oM = mapOrden(row, empresaId, usuarioId, evaluator, formatter);
                            batchOrden.add(oM);

                            if (batchOrden.size() == BATCH_SIZE) {
                                guardarOrden(batchOrden);
                                batchOrden.clear();
                            }
                        }

                        case "repuesto" -> {
                            Repuesto repuesto = mapRepuesto(row, empresaId);
                            batchRepuesto.add(repuesto);

                            if (batchRepuesto.size() == BATCH_SIZE) { // ✅ CORREGIDO
                                guardarRepuesto(batchRepuesto);
                                batchRepuesto.clear(); // ✅ CORREGIDO
                            }
                        }

                        case "ubicacion" -> {
                            Ubicacion ubicacion = mapUbicacion(row, empresaId);
                            batchUbicacion.add(ubicacion);

                            if (batchUbicacion.size() == BATCH_SIZE) { // ✅ CORREGIDO
                                guardarUbicacion(batchUbicacion);
                                batchUbicacion.clear(); // ✅ CORREGIDO
                            }
                        }

                        case "tipoActivo" -> {
                            TipoActivo tipoActivo = mapTipoActivo(row, empresaId);
                            batchTipoActivo.add(tipoActivo);

                            if (batchTipoActivo.size() == BATCH_SIZE) { // ✅ CORREGIDO
                                guardarTipoActivo(batchTipoActivo);
                                batchTipoActivo.clear(); // ✅ CORREGIDO
                            }
                        }

                        default -> throw new IllegalArgumentException("Tipo archivo inválido: " + archivo);
                    }

                    progressService.incrementar(jobId);

                } catch (IllegalArgumentException e) {
                    huboErrores = true; // 👈 clave
                    // errores de validación (datos malos)
                    progressService.error(
                        jobId,
                        row.getRowNum(),
                        e.getMessage(),
                        getRowData(row)
                    );

                } catch (Exception e) {
                    huboErrores = true; // 👈 clave
                    // errores inesperados
                    e.printStackTrace();
                    progressService.error(
                        jobId,
                        row.getRowNum(),
                        e.getMessage(),
                        getRowData(row)
                    );
                }
            }

            // 🔚 Guardar lo restante
            switch (archivo) {
                case "activo" -> {
                    if (!batchActivo.isEmpty()) guardarActivo(batchActivo, empresaId, usuarioId);
                }
                case "proveedor" -> {
                    if (!batchProveedor.isEmpty()) guardarProveedor(batchProveedor);
                }
                case "orden" -> {
                    if (!batchOrden.isEmpty()) guardarOrden(batchOrden);
                }
                case "repuesto" -> {
                    if (!batchRepuesto.isEmpty()) guardarRepuesto(batchRepuesto); // ✅ CORREGIDO
                }
                case "ubicacion" -> {
                    if (!batchUbicacion.isEmpty()) guardarUbicacion(batchUbicacion); // ✅ CORREGIDO
                }
                case "tipoActivo" -> {
                    if (!batchTipoActivo.isEmpty()) guardarTipoActivo(batchTipoActivo); // ✅ CORREGIDO
                }
            }

            ImportProgressDTO p = progressService.get(jobId);

            int procesados = p.getProcesados();
            p.setTotal(total);

            System.out.println("Total Filas: " + total);
            System.out.println("Total procesados: " + procesados);

            if (!huboErrores && procesados == total) {
                progressService.finalizar(jobId);
            } else if (procesados != 0) {
                progressService.finalizarConErrores(jobId);
            }

        } catch (IOException e) {
            huboErrores = true; // 👈 clave
            // error leyendo archivo
            e.printStackTrace();

        } catch (Exception e) {
            huboErrores = true; // 👈 clave
            // error general
            e.printStackTrace();

        } finally {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ex) {
                huboErrores = true; // 👈 clave
                ex.printStackTrace();
            }
        }
    }

    /**
     * Procesa de forma síncrona un lote de Activos ingresados manualmente
     * (grilla tipo planilla) desde el frontend. A diferencia de procesarAsync,
     * no usa jobId/polling: valida y guarda todo en la misma request y
     * devuelve el detalle fila por fila de una vez.
     */
    @Override
    @Transactional
    public ActivoImportResultDTO procesarActivosManual(List<ActivoImportRowDTO> filas, Long empresaId, Long usuarioId) {

        List<ActivoImportRowResultDTO> resultados = new ArrayList<>();
        List<Activo> validos = new ArrayList<>();
        Set<String> codigosEnLote = new HashSet<>();

        int fila = 0;

        for (ActivoImportRowDTO datos : filas) {

            fila++;

            try {

                String codigo = datos.getCodigoInterno();

                if (codigo != null && !codigo.isBlank() && !codigosEnLote.add(codigo)) {
                    throw new BusinessException("Código interno repetido en esta misma carga: " + codigo);
                }

                Activo activo = construirActivoDesdeCampos(
                    codigo,
                    datos.getNombre(),
                    datos.getDescripcion(),
                    datos.getTipoActivoNombre(),
                    datos.getMarca(),
                    datos.getModelo(),
                    datos.getNumeroSerie(),
                    datos.getFechaAdquisicion(),
                    datos.getValorAdquisicion(),
                    datos.getValorResidual(),
                    datos.getVidaUtilMeses(),
                    datos.getUbicacionNombre(),
                    datos.getProveedorRut(),
                    datos.getCuentaContable(),
                    empresaId
                );

                validos.add(activo);

                resultados.add(ActivoImportRowResultDTO.builder()
                    .fila(fila)
                    .exito(true)
                    .mensaje("OK")
                    .codigoInterno(codigo)
                    .build());

            } catch (Exception e) {

                resultados.add(ActivoImportRowResultDTO.builder()
                    .fila(fila)
                    .exito(false)
                    .mensaje(e.getMessage() != null ? e.getMessage() : "Error desconocido al procesar la fila")
                    .codigoInterno(datos.getCodigoInterno())
                    .build());
            }
        }

        if (!validos.isEmpty()) {
            try {
                guardarActivo(validos, empresaId, usuarioId);

                // Asociar el id ya generado a cada resultado exitoso (mismo orden en que se agregaron a "validos")
                int idx = 0;
                for (ActivoImportRowResultDTO r : resultados) {
                    if (r.isExito()) {
                        r.setActivoId(validos.get(idx).getId());
                        idx++;
                    }
                }
            } catch (Exception e) {
                // Si falla el guardado del lote (ej. una restricción de BD no detectada antes),
                // no perdemos el detalle ya calculado: marcamos como error las filas que iban a guardarse.
                for (ActivoImportRowResultDTO r : resultados) {
                    if (r.isExito()) {
                        r.setExito(false);
                        r.setMensaje("No se pudo guardar: " + (e.getMessage() != null ? e.getMessage() : "error desconocido"));
                    }
                }
            }
        }

        int exitosos = (int) resultados.stream().filter(ActivoImportRowResultDTO::isExito).count();

        return ActivoImportResultDTO.builder()
            .total(filas.size())
            .exitosos(exitosos)
            .fallidos(filas.size() - exitosos)
            .resultados(resultados)
            .build();
    }

    /**
     * Procesa de forma síncrona un lote de Proveedores ingresados manualmente
     * (grilla tipo planilla) desde el frontend.
     */
    @Override
    @Transactional
    public ImportBatchResultDTO procesarProveedoresManual(List<ProveedorImportRowDTO> filas, Long empresaId) {

        List<ImportRowResultDTO> resultados = new ArrayList<>();
        List<Proveedor> validos = new ArrayList<>();
        Set<String> rutsEnLote = new HashSet<>();
        Set<String> emailsEnLote = new HashSet<>();

        int fila = 0;

        for (ProveedorImportRowDTO datos : filas) {

            fila++;

            try {

                String rut = RutUtils.limpiarRut(datos.getRut());
                String email = datos.getEmail();

                if (rut != null && !rut.isBlank() && !rutsEnLote.add(rut)) {
                    throw new BusinessException("RUT repetido en esta misma carga: " + rut);
                }

                if (email != null && !email.isBlank() && !emailsEnLote.add(email.toLowerCase())) {
                    throw new BusinessException("Email repetido en esta misma carga: " + email);
                }

                Proveedor proveedor = construirProveedorDesdeCampos(
                    datos.getNombre(),
                    rut,
                    datos.getContacto(),
                    datos.getTelefono(),
                    datos.getEmail(),
                    datos.getTipoProveedor(),
                    empresaId
                );

                validos.add(proveedor);

                resultados.add(ImportRowResultDTO.builder()
                    .fila(fila).exito(true).mensaje("OK").referencia(rut).build());

            } catch (Exception e) {

                resultados.add(ImportRowResultDTO.builder()
                    .fila(fila).exito(false)
                    .mensaje(e.getMessage() != null ? e.getMessage() : "Error desconocido al procesar la fila")
                    .referencia(datos.getRut())
                    .build());
            }
        }

        if (!validos.isEmpty()) {
            try {
                guardarProveedor(validos);
                asignarIds(resultados, validos, Proveedor::getId);
            } catch (Exception e) {
                marcarLoteComoFallido(resultados, e);
            }
        }

        return construirResultadoLote(filas.size(), resultados);
    }

    /**
     * Procesa de forma síncrona un lote de Órdenes de Mantención ingresadas
     * manualmente (grilla tipo planilla) desde el frontend.
     */
    @Override
    @Transactional
    public ImportBatchResultDTO procesarOrdenesManual(List<OrdenImportRowDTO> filas, Long empresaId, Long usuarioId) {

        List<ImportRowResultDTO> resultados = new ArrayList<>();
        List<OrdenMantenimiento> validos = new ArrayList<>();

        int fila = 0;

        for (OrdenImportRowDTO datos : filas) {

            fila++;

            try {

                LocalDateTime fechaHora = null;

                if (datos.getFechaProgramada() != null && datos.getHoraProgramada() != null) {
                    fechaHora = LocalDateTime.of(datos.getFechaProgramada(), datos.getHoraProgramada());
                }

                OrdenMantenimiento orden = construirOrdenDesdeCampos(
                    datos.getTitulo(),
                    fechaHora,
                    datos.getDuracionMinutos(),
                    datos.getTipoMantenimiento(),
                    datos.getEstado(),
                    datos.getObservaciones(),
                    datos.getActivoNombre(),
                    datos.getProveedorRut(),
                    datos.getValorHoraProveedor(),
                    datos.getHorasEstimadasProveedor(),
                    datos.getCostoManoObraEstimadasProveedor(),
                    empresaId,
                    usuarioId
                );

                validos.add(orden);

                resultados.add(ImportRowResultDTO.builder()
                    .fila(fila).exito(true).mensaje("OK").referencia(datos.getTitulo()).build());

            } catch (Exception e) {

                resultados.add(ImportRowResultDTO.builder()
                    .fila(fila).exito(false)
                    .mensaje(e.getMessage() != null ? e.getMessage() : "Error desconocido al procesar la fila")
                    .referencia(datos.getTitulo())
                    .build());
            }
        }

        if (!validos.isEmpty()) {
            try {
                guardarOrden(validos);
                asignarIds(resultados, validos, OrdenMantenimiento::getId);
            } catch (Exception e) {
                marcarLoteComoFallido(resultados, e);
            }
        }

        return construirResultadoLote(filas.size(), resultados);
    }

    /**
     * Procesa de forma síncrona un lote de Repuestos ingresados manualmente
     * (grilla tipo planilla) desde el frontend.
     */
    @Override
    @Transactional
    public ImportBatchResultDTO procesarRepuestosManual(List<RepuestoImportRowDTO> filas, Long empresaId) {

        List<ImportRowResultDTO> resultados = new ArrayList<>();
        List<Repuesto> validos = new ArrayList<>();
        Set<String> codigosEnLote = new HashSet<>();

        int fila = 0;

        for (RepuestoImportRowDTO datos : filas) {

            fila++;

            try {

                String codigo = datos.getCodigo();

                if (codigo != null && !codigo.isBlank() && !codigosEnLote.add(codigo)) {
                    throw new BusinessException("Código repetido en esta misma carga: " + codigo);
                }

                Repuesto repuesto = construirRepuestoDesdeCampos(
                    codigo,
                    datos.getNombre(),
                    datos.getDescripcion(),
                    datos.getCosto(),
                    datos.getStockActual(),
                    datos.getStockMinimo(),
                    datos.getCuentaContable(),
                    datos.getTipoRepuesto(),
                    empresaId
                );

                validos.add(repuesto);

                resultados.add(ImportRowResultDTO.builder()
                    .fila(fila).exito(true).mensaje("OK").referencia(codigo).build());

            } catch (Exception e) {

                resultados.add(ImportRowResultDTO.builder()
                    .fila(fila).exito(false)
                    .mensaje(e.getMessage() != null ? e.getMessage() : "Error desconocido al procesar la fila")
                    .referencia(datos.getCodigo())
                    .build());
            }
        }

        if (!validos.isEmpty()) {
            try {
                guardarRepuesto(validos);
                asignarIds(resultados, validos, Repuesto::getId);
            } catch (Exception e) {
                marcarLoteComoFallido(resultados, e);
            }
        }

        return construirResultadoLote(filas.size(), resultados);
    }

    /**
     * Procesa de forma síncrona un lote de Ubicaciones ingresadas manualmente
     * (grilla tipo planilla) desde el frontend.
     */
    @Override
    @Transactional
    public ImportBatchResultDTO procesarUbicacionesManual(List<UbicacionImportRowDTO> filas, Long empresaId) {

        List<ImportRowResultDTO> resultados = new ArrayList<>();
        List<Ubicacion> validos = new ArrayList<>();

        int fila = 0;

        for (UbicacionImportRowDTO datos : filas) {

            fila++;

            try {

                Ubicacion ubicacion = construirUbicacionDesdeCampos(
                    datos.getNombre(),
                    datos.getDescripcion(),
                    datos.getDireccion(),
                    empresaId
                );

                validos.add(ubicacion);

                resultados.add(ImportRowResultDTO.builder()
                    .fila(fila).exito(true).mensaje("OK").referencia(datos.getNombre()).build());

            } catch (Exception e) {

                resultados.add(ImportRowResultDTO.builder()
                    .fila(fila).exito(false)
                    .mensaje(e.getMessage() != null ? e.getMessage() : "Error desconocido al procesar la fila")
                    .referencia(datos.getNombre())
                    .build());
            }
        }

        if (!validos.isEmpty()) {
            try {
                guardarUbicacion(validos);
                asignarIds(resultados, validos, Ubicacion::getId);
            } catch (Exception e) {
                marcarLoteComoFallido(resultados, e);
            }
        }

        return construirResultadoLote(filas.size(), resultados);
    }

    /**
     * Procesa de forma síncrona un lote de Tipos de Activo ingresados
     * manualmente (grilla tipo planilla) desde el frontend.
     */
    @Override
    @Transactional
    public ImportBatchResultDTO procesarTiposActivoManual(List<TipoActivoImportRowDTO> filas, Long empresaId) {

        List<ImportRowResultDTO> resultados = new ArrayList<>();
        List<TipoActivo> validos = new ArrayList<>();

        int fila = 0;

        for (TipoActivoImportRowDTO datos : filas) {

            fila++;

            try {

                TipoActivo tipoActivo = construirTipoActivoDesdeCampos(
                    datos.getNombre(),
                    datos.getDescripcion(),
                    datos.getVidaUtilReferencialMeses(),
                    empresaId
                );

                validos.add(tipoActivo);

                resultados.add(ImportRowResultDTO.builder()
                    .fila(fila).exito(true).mensaje("OK").referencia(datos.getNombre()).build());

            } catch (Exception e) {

                resultados.add(ImportRowResultDTO.builder()
                    .fila(fila).exito(false)
                    .mensaje(e.getMessage() != null ? e.getMessage() : "Error desconocido al procesar la fila")
                    .referencia(datos.getNombre())
                    .build());
            }
        }

        if (!validos.isEmpty()) {
            try {
                guardarTipoActivo(validos);
                asignarIds(resultados, validos, TipoActivo::getId);
            } catch (Exception e) {
                marcarLoteComoFallido(resultados, e);
            }
        }

        return construirResultadoLote(filas.size(), resultados);
    }

    // ==========================================================
    // Helpers genéricos para las cargas manuales (todas menos Activo,
    // que ya tenía su propio DTO de resultado antes de generalizar esto)
    // ==========================================================

    private <T> void asignarIds(List<ImportRowResultDTO> resultados, List<T> validos, java.util.function.Function<T, Long> idExtractor) {
        int idx = 0;
        for (ImportRowResultDTO r : resultados) {
            if (r.isExito()) {
                r.setId(idExtractor.apply(validos.get(idx)));
                idx++;
            }
        }
    }

    private void marcarLoteComoFallido(List<ImportRowResultDTO> resultados, Exception e) {
        for (ImportRowResultDTO r : resultados) {
            if (r.isExito()) {
                r.setExito(false);
                r.setMensaje("No se pudo guardar: " + (e.getMessage() != null ? e.getMessage() : "error desconocido"));
            }
        }
    }

    private ImportBatchResultDTO construirResultadoLote(int total, List<ImportRowResultDTO> resultados) {
        int exitosos = (int) resultados.stream().filter(ImportRowResultDTO::isExito).count();
        return ImportBatchResultDTO.builder()
            .total(total)
            .exitosos(exitosos)
            .fallidos(total - exitosos)
            .resultados(resultados)
            .build();
    }

    private boolean filaVacia(Row row) {

    if (row == null) {
        return true;
    }

    DataFormatter formatter = new DataFormatter();

    for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {

        if (i < 0) {
            return true;
        }

        Cell cell = row.getCell(i);

        if (cell == null) {
            continue;
        }

        String valor = formatter.formatCellValue(cell);

        if (!valor.trim().isEmpty()) {
            return false;
        }
    }

    return true;
}

    private String getRowData(Row row) {
        StringBuilder sb = new StringBuilder();

        for (Cell cell : row) {
            sb.append(cell.toString()).append(" | ");
        }

        return sb.toString();
    }

    private void guardarActivo(List<Activo> batch, Long empresaId, Long usuarioId) {
        List<Activo> activos = activoRepository.saveAll(batch);
        calcularYGuardarDepreciacionMensual(activos, empresaId);
        guardarHistoriaCracionActivo(activos, empresaId, usuarioId);
        em.flush();
        em.clear();
    }

    private void guardarProveedor(List<Proveedor> batch) {
        proveedorRepository.saveAll(batch);
        em.flush();
        em.clear();
    }

    private void guardarOrden(List<OrdenMantenimiento> batch) {
        oMantenimientoRepository.saveAll(batch);
        em.flush();
        em.clear();
    }

    private void guardarRepuesto(List<Repuesto> batch) {
        repuestoRepository.saveAll(batch);
        em.flush();
        em.clear();
    }

    private void guardarUbicacion(List<Ubicacion> batch) {
        ubicacionRepository.saveAll(batch);
        em.flush();
        em.clear();
    }

    private void guardarTipoActivo(List<TipoActivo> batch) {
        tipoActivoRepository.saveAll(batch);
        em.flush();
        em.clear();
    }

    private Activo mapActivo(Row row, Long empresaId) {

        String codigo = getString(row, 0);
        String nombre = getString(row, 1);
        String descripcion = getString(row, 2);
        String tipoActivoNombre = getString(row, 3);
        String marca = getString(row, 4);
        String modelo = getString(row, 5);
        String numeroSerie = getString(row, 6);
        LocalDate fechaAdquisicion = getLocalDate(row, 7);
        BigDecimal valorAdquisicion = getBigDecimal(row, 8);
        BigDecimal valorResidual = getBigDecimal(row, 9);
        Integer vidaUtilMeses = getInteger(row, 10);
        String ubicacionNombre = getString(row, 11);
        String proveedorRut = getString(row, 12);
        String cuentaContable = getString(row, 13);

        return construirActivoDesdeCampos(
            codigo,
            nombre,
            descripcion,
            tipoActivoNombre,
            marca,
            modelo,
            numeroSerie,
            fechaAdquisicion,
            valorAdquisicion,
            valorResidual,
            vidaUtilMeses,
            ubicacionNombre,
            proveedorRut,
            cuentaContable,
            empresaId
        );
    }

    private Proveedor mapProveedor(Row row, Long empresaId) {

        String nombre = getRequiredString(row, 0, "El nombre del proveedor es obligatorio");
        String rut = getRequiredString(row, 1, "El RUT del proveedor es obligatorio");
        String contacto = getString(row, 2);
        String telefono = getString(row, 3);
        String email = getString(row, 4);
        String tipoProveedor = getString(row, 5);

        return construirProveedorDesdeCampos(nombre, rut, contacto, telefono, email, tipoProveedor, empresaId);
    }

    private OrdenMantenimiento mapOrden(Row row, Long empresaId, Long usuarioId, FormulaEvaluator evaluator,
        DataFormatter formatter) {

        String titulo = getRequiredString(row, 0, "El título es obligatorio");

        // 🔥 fecha + hora separadas en el Excel
        LocalDateTime fechaProgramada = getDateTimeSafe(
            row,
            1, // columna fecha
            2, // columna hora
            "Fecha u hora inválida"
        );

        Integer duracionMinutos = getInteger(row, 3, "Duración minutos inválido");
        String tipoMantenimiento = getRequiredString(row, 4, "Tipo de mantenimiento inválido");
        String estado = getRequiredString(row, 5, "Estado inválido");
        String observaciones = getString(row, 6);
        String activoNombre = getRequiredString(row, 7, "Activo requerido");
        String proveedorRut = getString(row, 8);
        BigDecimal valorHora = getBigDecimal(row, 9, "Valor Hora inválido");
        BigDecimal horaEstimada = getBigDecimal(row, 10, "Hora Estimada inválido", evaluator, formatter);
        BigDecimal costoManoObraEstimada = getBigDecimal(row, 11, "Costo mano de obra estimada inválido", evaluator, formatter);

        return construirOrdenDesdeCampos(
            titulo,
            fechaProgramada,
            duracionMinutos,
            tipoMantenimiento,
            estado,
            observaciones,
            activoNombre,
            proveedorRut,
            valorHora,
            horaEstimada,
            costoManoObraEstimada,
            empresaId,
            usuarioId
        );
    }

    private Repuesto mapRepuesto(Row row, Long empresaId) {

        String codigo = getRequiredString(row, 0, "El código es obligatorio");
        String nombre = getRequiredString(row, 1, "El nombre es obligatorio");
        String descripcion = getString(row, 2);

        BigDecimal costo = getBigDecimal(row, 3, "Costo inválido");
        Integer stockActual = getInteger(row, 4, "Stock Actual inválido");
        Integer stockMinimo = getInteger(row, 5, "Stock mínimo inválido");

        String cuentaContable = getString(row, 6);
        String tipoRepuesto = getString(row, 7);

        return construirRepuestoDesdeCampos(codigo, nombre, descripcion, costo, stockActual, stockMinimo, cuentaContable, tipoRepuesto, empresaId);
    }

    private Ubicacion mapUbicacion(Row row, Long empresaId) {

        String nombre = getRequiredString(row, 0, "El nombre es obligatorio");
        String descripcion = getString(row, 1);
        String direccion = getString(row, 2);

        return construirUbicacionDesdeCampos(nombre, descripcion, direccion, empresaId);
    }

    private TipoActivo mapTipoActivo(Row row, Long empresaId) {

        String nombre = getRequiredString(row, 0, "El nombre es obligatorio");
        String descripcion = getString(row, 1);
        Integer vidaUtil = getInteger(row, 2);

        return construirTipoActivoDesdeCampos(nombre, descripcion, vidaUtil, empresaId);
    }

    /**
     * Construye y valida un Activo a partir de campos ya tipados (sin depender
     * de Apache POI). La usan tanto el importador de Excel (mapActivo, que
     * primero extrae estos valores desde la Row) como la carga manual
     * (procesarActivosManual, que los recibe directo como JSON), para no
     * duplicar reglas de negocio entre ambos caminos.
     */
    private Activo construirActivoDesdeCampos(
        String codigo,
        String nombre,
        String descripcion,
        String tipoActivoNombre,
        String marca,
        String modelo,
        String numeroSerie,
        LocalDate fechaAdquisicion,
        BigDecimal valorAdquisicion,
        BigDecimal valorResidual,
        Integer vidaUtilMeses,
        String ubicacionNombre,
        String proveedorRut,
        String cuentaContable,
        Long empresaId
    ) {

        if (codigo == null || codigo.isBlank()) {
            throw new BusinessException("El código interno es obligatorio");
        }

        validarCodigoUnico(codigo);

        TipoActivo tipoActivo = obtenerTipoActivo(tipoActivoNombre, empresaId);
        Ubicacion ubicacion = obtenerUbicacion(ubicacionNombre, empresaId);
        Proveedor proveedor = obtenerProveedor(proveedorRut);
        Empresa empresa = obtenerEmpresa(empresaId);

        Activo activo = new Activo();

        activo.setCodigoInterno(codigo);
        activo.setNombre(nombre);
        activo.setDescripcion(descripcion);
        activo.setTipoActivo(tipoActivo);
        activo.setMarca(marca);
        activo.setModelo(modelo);
        activo.setNumeroSerie(numeroSerie);
        activo.setFechaAdquisicion(fechaAdquisicion);
        activo.setValorAdquisicion(valorAdquisicion != null ? valorAdquisicion : BigDecimal.ZERO);
        activo.setValorResidual(valorResidual != null ? valorResidual : BigDecimal.ZERO);
        activo.setVidaUtilMeses(vidaUtilMeses != null ? vidaUtilMeses : 0);
        activo.setCuentaContable(cuentaContable != null ? cuentaContable : "0");
        activo.setEstadoActual(EstadoActivo.OPERATIVO);
        activo.setFechaCreacion(LocalDateTime.now());
        activo.setUbicacion(ubicacion);
        activo.setProveedor(proveedor);
        activo.setEmpresa(empresa);

        return activo;
    }

    /**
     * Construye y valida un Proveedor a partir de campos ya tipados. La usan
     * tanto el importador de Excel (mapProveedor) como la carga manual
     * (procesarProveedoresManual).
     */
    private Proveedor construirProveedorDesdeCampos(
        String nombre,
        String rut,
        String contacto,
        String telefono,
        String email,
        String tipoProveedorStr,
        Long empresaId
    ) {

        // 🔒 Si el RUT viene con puntos (ej: "12.345.678-9"), se guarda
        // sin puntos (ej: "12345678-9"), igual que en la creación
        // individual de proveedor.
        rut = RutUtils.limpiarRut(rut);

        if (nombre == null || nombre.isBlank()) {
            throw new BusinessException("El nombre del proveedor es obligatorio");
        }

        if (rut == null || rut.isBlank()) {
            throw new BusinessException("El RUT del proveedor es obligatorio");
        }

        validarRutUnico(rut);
        validarEmailUnicoProveedor(email);

        Empresa empresa = obtenerEmpresa(empresaId);
        TipoProveedor tipoProveedor = parseEnumOrThrow(TipoProveedor.class, tipoProveedorStr, "Tipo de proveedor inválido");

        Proveedor proveedor = new Proveedor();

        proveedor.setNombre(nombre);
        proveedor.setRut(rut);
        proveedor.setContacto(contacto);
        proveedor.setTelefono(telefono);
        proveedor.setEmail(email);
        proveedor.setTipoProveedor(tipoProveedor);
        proveedor.setEmpresa(empresa);
        proveedor.setActivo(true);

        return proveedor;
    }

    /**
     * Construye y valida una Orden de Mantención a partir de campos ya
     * tipados. La usan tanto el importador de Excel (mapOrden) como la carga
     * manual (procesarOrdenesManual).
     */
    private OrdenMantenimiento construirOrdenDesdeCampos(
        String titulo,
        LocalDateTime fechaProgramada,
        Integer duracionMinutos,
        String tipoMantenimientoStr,
        String estadoStr,
        String observaciones,
        String activoNombre,
        String proveedorRut,
        BigDecimal valorHora,
        BigDecimal horasEstimada,
        BigDecimal costoManoObraEstimada,
        Long empresaId,
        Long usuarioId
    ) {

        if (titulo == null || titulo.isBlank()) {
            throw new BusinessException("El título es obligatorio");
        }

        if (fechaProgramada == null) {
            throw new BusinessException("Fecha u hora inválida");
        }

        if (duracionMinutos == null) {
            throw new BusinessException("Duración minutos inválido");
        }

        if (valorHora == null) {
            throw new BusinessException("Valor Hora inválido");
        }

        Empresa empresa = obtenerEmpresa(empresaId);
        Usuario usuario = obtenerUsuario(usuarioId);
        TipoMantenimiento tipo = parseEnumOrThrow(TipoMantenimiento.class, tipoMantenimientoStr, "Tipo de mantenimiento inválido");
        EstadoOrden estado = parseEnumOrThrow(EstadoOrden.class, estadoStr, "Estado inválido");

        if (activoNombre == null || activoNombre.isBlank()) {
            throw new BusinessException("Activo requerido");
        }

        Activo activo = obtenerActivoPorNombreYEmpresa(activoNombre, empresaId);
        Proveedor proveedor = obtenerProveedor(proveedorRut);

        OrdenMantenimiento o = new OrdenMantenimiento();

        o.setTitulo(titulo);
        o.setFechaProgramada(fechaProgramada);
        o.setFechaTermino(fechaProgramada.plusMinutes(duracionMinutos));
        o.setDuracionSegundos(Long.valueOf(duracionMinutos * 60));
        o.setTipoMantenimiento(tipo);
        o.setEstado(estado);
        o.setObservaciones(observaciones);
        o.setActivo(activo);
        o.setUsuario(usuario);
        o.setProveedor(proveedor);
        o.setValorHoraProveedor(valorHora);
        o.setHorasEstimadasProveedor(horasEstimada);
        o.setCostoManoObraEstimadasProveedor(costoManoObraEstimada);
        o.setEmpresa(empresa);

        return o;
    }

    /**
     * Construye y valida un Repuesto a partir de campos ya tipados. La usan
     * tanto el importador de Excel (mapRepuesto) como la carga manual
     * (procesarRepuestosManual).
     */
    private Repuesto construirRepuestoDesdeCampos(
        String codigo,
        String nombre,
        String descripcion,
        BigDecimal costo,
        Integer stockActual,
        Integer stockMinimo,
        String cuentaContable,
        String tipoRepuestoStr,
        Long empresaId
    ) {

        if (codigo == null || codigo.isBlank()) {
            throw new BusinessException("El código es obligatorio");
        }

        if (nombre == null || nombre.isBlank()) {
            throw new BusinessException("El nombre es obligatorio");
        }

        if (costo == null) {
            throw new BusinessException("Costo inválido");
        }

        if (stockActual == null) {
            throw new BusinessException("Stock Actual inválido");
        }

        if (stockMinimo == null) {
            throw new BusinessException("Stock mínimo inválido");
        }

        Empresa empresa = obtenerEmpresa(empresaId);
        validarCodigoUnico(codigo, empresa);

        TipoRepuesto tipoRepuesto = parseEnumOrThrow(TipoRepuesto.class, tipoRepuestoStr, "Tipo de repuesto inválido");

        Repuesto repuesto = new Repuesto();

        repuesto.setCodigo(codigo);
        repuesto.setNombre(nombre);
        repuesto.setDescripcion(descripcion);
        repuesto.setCostoUnitario(costo);
        repuesto.setStockActual(stockActual);
        repuesto.setStockMinimo(stockMinimo);
        repuesto.setCuentaContable(cuentaContable);
        repuesto.setTipo(tipoRepuesto);
        repuesto.setActivo(true);
        repuesto.setEmpresa(empresa);

        return repuesto;
    }

    /**
     * Construye y valida una Ubicación a partir de campos ya tipados. La usan
     * tanto el importador de Excel (mapUbicacion) como la carga manual
     * (procesarUbicacionesManual).
     */
    private Ubicacion construirUbicacionDesdeCampos(
        String nombre,
        String descripcion,
        String direccion,
        Long empresaId
    ) {

        if (nombre == null || nombre.isBlank()) {
            throw new BusinessException("El nombre es obligatorio");
        }

        Empresa empresa = obtenerEmpresa(empresaId);

        Ubicacion ubicacion = new Ubicacion();

        ubicacion.setNombre(nombre);
        ubicacion.setDescripcion(descripcion);
        ubicacion.setDireccion(direccion);
        ubicacion.setActivo(true);
        ubicacion.setEmpresa(empresa);

        return ubicacion;
    }

    /**
     * Construye y valida un Tipo de Activo a partir de campos ya tipados. La
     * usan tanto el importador de Excel (mapTipoActivo) como la carga manual
     * (procesarTiposActivoManual).
     */
    private TipoActivo construirTipoActivoDesdeCampos(
        String nombre,
        String descripcion,
        Integer vidaUtil,
        Long empresaId
    ) {

        if (nombre == null || nombre.isBlank()) {
            throw new BusinessException("El nombre es obligatorio");
        }

        Empresa empresa = obtenerEmpresa(empresaId);

        TipoActivo tipoActivo = new TipoActivo();

        tipoActivo.setNombre(nombre);
        tipoActivo.setDescripcion(descripcion);
        tipoActivo.setVidaUtilReferencialMeses(vidaUtil != null ? vidaUtil : 0);
        tipoActivo.setActivo(true);
        tipoActivo.setEmpresa(empresa);

        return tipoActivo;
    }

    private void validarCodigoUnico(String codigo) {
        if (activoRepository.existsByCodigoInterno(codigo)) {
            throw new BusinessException("El código interno ya existe: " + codigo);
        }
    }

    private TipoActivo obtenerTipoActivo(String nombre, Long empresaId) {
        return tipoActivoRepository.findFirstByNombreAndEmpresaId(nombre, empresaId)
            .orElseThrow(() -> new BusinessException("Tipo de activo no existe: " + nombre));
    }

    private Ubicacion obtenerUbicacion(String nombre, Long empresaId) {
        return ubicacionRepository.findFirstByNombreAndEmpresaId(nombre, empresaId)
            .orElseThrow(() -> new BusinessException("Ubicación no existe: " + nombre));
    }

    private Proveedor obtenerProveedor(String rut) {
        return proveedorRepository.findByRut(rut)
            .orElseThrow(() -> new BusinessException("Proveedor no existe: " + rut));
    }

    private Empresa obtenerEmpresa(Long empresaId) {
        return empresaRepository.findById(empresaId)
            .orElseThrow(() -> new BusinessException("Empresa no encontrada"));
    }

    private Activo obtenerActivoPorNombreYEmpresa(String nombre, Long empresaId) {
        return activoRepository.findFirstByNombreAndEmpresaId(nombre, empresaId)
            .orElseThrow(() -> new BusinessException("Activo no existe: " + nombre));
    }

    private Usuario obtenerUsuario(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new BusinessException("Usuario no existe: " + usuarioId));
    }

    private String getString(Row row, int index) {
        Cell cell = row.getCell(index);

        if (cell == null) return null;

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default -> throw new BusinessException("Tipo de celda inválido en columna " + index);
        };
    }

    private LocalDate getLocalDate(Row row, int index) {
        try {
            String valor = getString(row, index);

            // 🔒 Celda vacía: se asume "hoy" en vez de fallar la fila.
            if (valor == null || valor.isBlank()) {
                return LocalDate.now();
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

            return LocalDate.parse(valor, formatter);

        } catch (DateTimeParseException e) {
            // 🔒 Formato inválido (ej: "31/13/2025", "no-es-una-fecha"): en
            // vez de rechazar toda la fila con un BusinessException, se usa
            // la fecha actual del servidor. Un dato mal tipeado en la fecha
            // de adquisición no debería bloquear el ingreso del activo.
            return LocalDate.now();
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private BigDecimal getBigDecimal(Row row, int index) {
        Cell cell = row.getCell(index);

        if (cell == null) return BigDecimal.ZERO;

        return switch (cell.getCellType()) {
            case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue());
            case STRING -> new BigDecimal(cell.getStringCellValue());
            default -> throw new BusinessException("Número inválido en columna " + index);
        };
    }

    private Integer getInteger(Row row, int index) {
        return getBigDecimal(row, index).intValue();
    }

    private BigDecimal getBigDecimal(Row row, int index, String mensaje) {

        Cell cell = row.getCell(index);

        if (cell == null) {
            throw new BusinessException(mensaje + " (columna " + index + ")");
        }

        try {
            return switch (cell.getCellType()) {
                case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue());
                case STRING -> new BigDecimal(cell.getStringCellValue());
                default -> throw new BusinessException(mensaje + " (columna " + index + ")");
            };
        } catch (Exception e) {
            throw new BusinessException(mensaje + " (columna " + index + ")");
        }
    }

    private BigDecimal getBigDecimal(
    Row row,
    int col,
    String error,
    FormulaEvaluator evaluator,
    DataFormatter formatter
    ) {

        String valor = formatter
                .formatCellValue(row.getCell(col), evaluator)
                .trim();

        if (valor.isBlank()) {
            return null;
        }

        try {
            return new BigDecimal(valor.replace(",", "."));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(error);
        }
    }

    private Integer getInteger(Row row, int index, String mensaje) {
        return getBigDecimal(row, index, mensaje).intValue();
    }

    private String getRequiredString(Row row, int index, String mensajeError) {
        String value = getString(row, index);

        if (value == null || value.isBlank()) {
            throw new BusinessException(mensajeError + " (columna " + index + ")");
        }

        return value;
    }

    private void validarRutUnico(String rut) {
        if (proveedorRepository.existsByRut(rut)) {
            throw new BusinessException("Ya existe un proveedor con ese RUT: " + rut);
        }
    }

    private void validarEmailUnicoProveedor(String email) {

        if (email == null || email.isBlank()) return;

        if (proveedorRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("Ya existe un proveedor con ese email: " + email);
        }
    }

        private void validarCodigoUnico(String codigo, Empresa empresa) {

        if (repuestoRepository.existsByCodigoAndEmpresa(codigo, empresa)) {
            throw new BusinessException("El código ya existe: " + codigo);
        }
    }

    /**
     * Convierte un String (venga de una celda de Excel o de la grilla manual)
     * en el enum correspondiente, lanzando un BusinessException con un
     * mensaje claro si no es válido. La usan los métodos "construirXDesdeCampos"
     * compartidos por ambos caminos de carga.
     */
    private <T extends Enum<T>> T parseEnumOrThrow(Class<T> enumClass, String valor, String mensajeError) {

        if (valor == null || valor.isBlank()) {
            throw new BusinessException(mensajeError + " (vacío)");
        }

        try {
            return Enum.valueOf(enumClass, valor.trim().toUpperCase());
        } catch (Exception e) {
            throw new BusinessException(mensajeError + ": " + valor);
        }
    }

    private LocalDateTime getDateTimeSafe(
        Row row,
        int colFecha,
        int colHora,
        String mensaje
    ) {

        Cell fechaCell = row.getCell(colFecha);
        Cell horaCell = row.getCell(colHora);

        if (fechaCell == null || horaCell == null) {
            throw new BusinessException(mensaje);
        }

        try {
            LocalDate fecha;
            LocalTime hora;

            // 📅 FECHA
            if (fechaCell.getCellType() == CellType.NUMERIC) {
                fecha = fechaCell.getLocalDateTimeCellValue().toLocalDate();
            } else {
                fecha = LocalDate.parse(
                    getString(row, colFecha),
                    DateTimeFormatter.ofPattern("dd-MM-yyyy")
                );
            }

            // ⏱️ HORA
            if (horaCell.getCellType() == CellType.NUMERIC) {
                hora = horaCell.getLocalDateTimeCellValue().toLocalTime();
            } else {
                hora = LocalTime.parse(getString(row, colHora));
            }

            return LocalDateTime.of(fecha, hora);

        } catch (Exception e) {
            throw new BusinessException(mensaje + " (columnas " + colFecha + ", " + colHora + ")");
        }
    }

    private void calcularYGuardarDepreciacionMensual(List<Activo> activos, Long empresaId) {
        for (Activo activo : activos) {
            depreciacionService.calcularYGuardarDepreciacionMensual(activo, empresaId);
        }
    }

    private void guardarHistoriaCracionActivo(List<Activo> activos, Long empresaId, Long usuarioId) {
        for (Activo activo : activos) {
            historialEstadoActivoService.registrarCambioEstado(
                activo.getId(), EstadoActivo.OPERATIVO, null, "Creación de Activo", usuarioId
            );


        }
    }

}
