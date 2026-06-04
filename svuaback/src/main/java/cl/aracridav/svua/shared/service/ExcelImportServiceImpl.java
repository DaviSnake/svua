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
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
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
import cl.aracridav.svua.inventario.activo.entity.Activo;
import cl.aracridav.svua.inventario.activo.repository.ActivoRepository;
import cl.aracridav.svua.inventario.tipoactivo.entity.TipoActivo;
import cl.aracridav.svua.inventario.tipoactivo.repository.TipoActivoRepository;
import cl.aracridav.svua.inventario.ubicacion.entity.Ubicacion;
import cl.aracridav.svua.inventario.ubicacion.repository.UbicacionRepository;
import cl.aracridav.svua.mantenimiento.orden.entity.EstadoOrden;
import cl.aracridav.svua.mantenimiento.orden.entity.OrdenMantenimiento;
import cl.aracridav.svua.mantenimiento.orden.repository.OrdenMantenimientoRepository;
import cl.aracridav.svua.mantenimiento.plan.entity.PlanMantenimiento;
import cl.aracridav.svua.mantenimiento.plan.entity.TipoMantenimiento;
import cl.aracridav.svua.mantenimiento.plan.repository.PlanMantenimientoRepository;
import cl.aracridav.svua.mantenimiento.repuesto.entity.Repuesto;
import cl.aracridav.svua.mantenimiento.repuesto.entity.TipoRepuesto;
import cl.aracridav.svua.mantenimiento.repuesto.repository.RepuestoRepository;
import cl.aracridav.svua.proveedor.entity.Proveedor;
import cl.aracridav.svua.proveedor.entity.TipoProveedor;
import cl.aracridav.svua.proveedor.repository.ProveedorRepository;
import cl.aracridav.svua.shared.dto.response.ImportProgressDTO;
import cl.aracridav.svua.shared.enums.EstadoActivo;
import cl.aracridav.svua.shared.exception.BusinessException;
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
    private final UsuarioRepository usuarioRepository;
    private final PlanMantenimientoRepository planRepository;
    private final TipoActivoRepository tipoActivoRepository;
    private final UbicacionRepository ubicacionRepository;
    private final ProveedorRepository proveedorRepository;
    private final ImportProgressService progressService;

    @PersistenceContext
    private EntityManager em;

    private static final int BATCH_SIZE = 100;

    @Async
    @Transactional
    public void procesarAsync(Path path, String jobId, Long empresaId, Long usuarioId, String archivo) {

        boolean huboErrores = false;

        try (InputStream is = Files.newInputStream(path);
            Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            int total = sheet.getLastRowNum();

            progressService.iniciar(jobId, total);

            List<Activo> batchActivo = new ArrayList<>();
            List<Proveedor> batchProveedor = new ArrayList<>();
            List<OrdenMantenimiento> batchOrden = new ArrayList<>();
            List<Repuesto> batchRepuesto = new ArrayList<>();

            for (Row row : sheet) {

                if (row.getRowNum() == 0) continue;

                try {
                    switch (archivo) {

                        case "activo" -> {
                            Activo activo = mapActivo(row, empresaId);
                            batchActivo.add(activo);

                            if (batchActivo.size() == BATCH_SIZE) {
                                guardarActivo(batchActivo, empresaId);
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
                            OrdenMantenimiento oM = mapOrden(row, empresaId, usuarioId);
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
                    if (!batchActivo.isEmpty()) guardarActivo(batchActivo, empresaId);
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
            }

            ImportProgressDTO p = progressService.get(jobId);

            int procesados = p.getProcesados();

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

    private String getRowData(Row row) {
        StringBuilder sb = new StringBuilder();

        for (Cell cell : row) {
            sb.append(cell.toString()).append(" | ");
        }

        return sb.toString();
    }
    
    private void guardarActivo(List<Activo> batch, Long empresaId) {
        List<Activo> activos = activoRepository.saveAll(batch);
        calcularYGuardarDepreciacionMensual(activos, empresaId);
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

    private Activo mapActivo(Row row, Long empresaId) {

        String codigo = getString(row, 0);
        validarCodigoUnico(codigo);

        TipoActivo tipoActivo = obtenerTipoActivo(getString(row, 3));
        Ubicacion ubicacion = obtenerUbicacion(getString(row, 11));
        Proveedor proveedor = obtenerProveedor(getString(row, 12));
        Empresa empresa = obtenerEmpresa(empresaId);

        return construirActivo(row, codigo, tipoActivo, ubicacion, proveedor, empresa);
    }

    private Proveedor mapProveedor(Row row, Long empresaId) {

        String nombre = getRequiredString(row, 0, "El nombre del proveedor es obligatorio");
        String rut = getRequiredString(row, 1, "El RUT del proveedor es obligatorio");

        validarRutUnico(rut);

        Empresa empresa = obtenerEmpresa(empresaId);

        return construirProveedor(row, nombre, rut, empresa);
    }

    private OrdenMantenimiento mapOrden(Row row, Long empresaId, Long usuarioId) {

        Empresa empresa = obtenerEmpresa(empresaId);
        Usuario usuario = obtenerUsuario(usuarioId);
        

        String titulo = getRequiredString(row, 0, "El título es obligatorio");

        // 🔥 NUEVO: fecha + hora separadas
        LocalDateTime fechaProgramada = getDateTimeSafe(
            row,
            1, // columna fecha
            2, // columna hora
            "Fecha u hora inválida"
        );

        Integer duracionMinutos = getInteger(row, 3, "Duración minutos inválido");

        TipoMantenimiento tipo = getEnum(
            row, 4, TipoMantenimiento.class, "Tipo de mantenimiento inválido"
        );

        EstadoOrden estado = getEnum(
            row, 5, EstadoOrden.class, "Estado inválido"
        );

        String observaciones = getString(row, 6);

        Activo activo = obtenerActivoPorNombre(getRequiredString(row, 7, "Activo requerido"));
        Proveedor proveedor = obtenerProveedor(getString(row, 8));
        BigDecimal valorHora = getBigDecimal(row, 9, "Valor Hora inválido");
        BigDecimal horaEstimada = getBigDecimal(row, 10, "Hora Estimada inválido");
        BigDecimal costoManoObraEstimada = getBigDecimal(row, 11, "Costo mano de obra estimada inválido");
        PlanMantenimiento plan = obtenerPlan(Long.valueOf(1));

        return construirOrden(
            titulo,
            fechaProgramada,
            duracionMinutos,
            tipo,
            estado,
            observaciones,
            activo,
            usuario,
            proveedor,
            valorHora,
            horaEstimada,
            costoManoObraEstimada,
            plan,
            empresa
        );
    }

    private Repuesto mapRepuesto(Row row, Long empresaId) {

        Empresa empresa = obtenerEmpresa(empresaId);

        String codigo = getRequiredString(row, 0, "El código es obligatorio");
        validarCodigoUnico(codigo, empresa);

        String nombre = getRequiredString(row, 1, "El nombre es obligatorio");
        String descripcion = getString(row, 2);

        BigDecimal costo = getBigDecimal(row, 3, "Costo inválido");
        Integer StockActual = getInteger(row, 4, "Stock Actual inválido");
        Integer stockMinimo = getInteger(row, 5, "Stock mínimo inválido");

        String cuentaContable = getString(row, 6);
        TipoRepuesto tipoRepuesto = TipoRepuesto.valueOf(
            row.getCell(7)
            .getStringCellValue()
            .trim()
            .toUpperCase()
        );


        return construirRepuesto(codigo, nombre, descripcion, costo, StockActual, stockMinimo, cuentaContable, tipoRepuesto, empresa);
    }

    private void validarCodigoUnico(String codigo) {
        if (activoRepository.existsByCodigoInterno(codigo)) {
            throw new BusinessException("El código interno ya existe: " + codigo);
        }
    }

    private Activo construirActivo(
        Row row,
        String codigo,
        TipoActivo tipoActivo,
        Ubicacion ubicacion,
        Proveedor proveedor,
        Empresa empresa
    ) {

        Activo activo = new Activo();

        activo.setCodigoInterno(codigo);
        activo.setNombre(getString(row, 1));
        activo.setDescripcion(getString(row, 2));
        activo.setTipoActivo(tipoActivo);
        activo.setMarca(getString(row, 4));
        activo.setModelo(getString(row, 5));
        activo.setNumeroSerie(getString(row, 6));
        activo.setFechaAdquisicion(getLocalDate(row, 7));
        activo.setValorAdquisicion(getBigDecimal(row, 8));
        activo.setValorResidual(getBigDecimal(row, 9));
        activo.setVidaUtilMeses(getInteger(row, 10));
        activo.setCuentaContable(getString(row, 13));
        activo.setEstadoActual(EstadoActivo.OPERATIVO);
        activo.setUbicacion(ubicacion);
        activo.setProveedor(proveedor);
        activo.setEmpresa(empresa);

        return activo;
    }

    private Proveedor construirProveedor(
        Row row,
        String nombre,
        String rut,
        Empresa empresa
    ) {

        Proveedor proveedor = new Proveedor();

        proveedor.setNombre(nombre);
        proveedor.setRut(rut);
        proveedor.setContacto(getString(row, 2));
        proveedor.setTelefono(getString(row, 3));
        proveedor.setEmail(getString(row, 4));
        proveedor.setTipoProveedor(
            TipoProveedor.valueOf(
                row.getCell(5)
                .getStringCellValue()
                .trim()
                .toUpperCase()
            )
        );
        proveedor.setEmpresa(empresa);
        proveedor.setActivo(true);

        return proveedor;
    }

    private OrdenMantenimiento construirOrden(
        String titulo,
        LocalDateTime fechaProgramada,
        Integer duracionMinutos,
        TipoMantenimiento tipo,
        EstadoOrden estado,
        String observaciones,
        Activo activo,
        Usuario usuario,
        Proveedor proveedor,
        BigDecimal valorHora,
        BigDecimal horasEstimada,
        BigDecimal costoManoObraEstimada,
        PlanMantenimiento plan,
        Empresa empresa
    ) {

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
        o.setPlanMantenimiento(plan);
        o.setEmpresa(empresa);

        return o;
    }

    private Repuesto construirRepuesto(
        String codigo,
        String nombre,
        String descripcion,
        BigDecimal costo,
        Integer stockActual,
        Integer stockMinimo,
        String cuentaContable,
        TipoRepuesto tipoRepuesto,
        Empresa empresa
    ) {

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

    private TipoActivo obtenerTipoActivo(String nombre) {
        return tipoActivoRepository.findFirstByNombre(nombre)
            .orElseThrow(() -> new BusinessException("Tipo de activo no existe: " + nombre));
    }

    private Ubicacion obtenerUbicacion(String nombre) {
        return ubicacionRepository.findFirstByNombre(nombre)
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

    private Activo obtenerActivoPorNombre(String nombre) {
        return activoRepository.findFirstByNombre(nombre)
            .orElseThrow(() -> new BusinessException("Activo no existe: " + nombre));
    }

    private Usuario obtenerUsuario(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new BusinessException("Usuario no existe: " + usuarioId));
    }

    private PlanMantenimiento obtenerPlan(Long id) {
        return planRepository.findById(id)
            .orElseThrow(() -> new BusinessException("Plan mantenimiento no encontrado: " + id));
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

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

            return LocalDate.parse(valor, formatter);

        } catch (DateTimeParseException e) {
            throw new BusinessException("Fecha inválida en columna " + index + ". Formato esperado: dd-MM-yyyy");
        } catch (Exception e) {
            throw new BusinessException("Error al leer fecha en columna " + index);
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

        private void validarCodigoUnico(String codigo, Empresa empresa) {

        if (repuestoRepository.existsByCodigoAndEmpresa(codigo, empresa)) {
            throw new BusinessException("El código ya existe: " + codigo);
        }
    }

    /*private Long getRequiredLong(Row row, int index, String mensaje) {

        Cell cell = row.getCell(index);

        if (cell == null) {
            throw new BusinessException(mensaje + " (columna " + index + ")");
        }

        return switch (cell.getCellType()) {
            case NUMERIC -> (long) cell.getNumericCellValue();
            case STRING -> Long.parseLong(cell.getStringCellValue());
            default -> throw new BusinessException("Número inválido en columna " + index);
        };
    }*/

    private <T extends Enum<T>> T getEnum(
        Row row,
        int index,
        Class<T> enumClass,
        String mensaje
    ) {
        try {
            String value = getRequiredString(row, index, mensaje);
            return Enum.valueOf(enumClass, value.trim().toUpperCase());
        } catch (Exception e) {
            throw new BusinessException(mensaje + " (columna " + index + ")");
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

}
