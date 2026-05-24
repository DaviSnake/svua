package cl.aracridav.svua.inventario.activo.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import cl.aracridav.svua.depreciacion.service.DepreciacionService;
import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.empresa.repository.EmpresaRepository;
import cl.aracridav.svua.inventario.activo.dto.request.ActivoCreateRequest;
import cl.aracridav.svua.inventario.activo.dto.request.ActivoUpdateRequest;
import cl.aracridav.svua.inventario.activo.dto.request.DarDeBajaActivoRequest;
import cl.aracridav.svua.inventario.activo.dto.response.ActivoResponse;
import cl.aracridav.svua.inventario.activo.entity.Activo;
import cl.aracridav.svua.inventario.activo.repository.ActivoRepository;
import cl.aracridav.svua.inventario.historial.service.HistorialEstadoActivoService;
import cl.aracridav.svua.inventario.tipoactivo.entity.TipoActivo;
import cl.aracridav.svua.inventario.tipoactivo.repository.TipoActivoRepository;
import cl.aracridav.svua.inventario.ubicacion.entity.Ubicacion;
import cl.aracridav.svua.inventario.ubicacion.repository.UbicacionRepository;
import cl.aracridav.svua.mantenimiento.orden.entity.OrdenMantenimiento;
import cl.aracridav.svua.mantenimiento.orden.repository.OrdenMantenimientoRepository;
import cl.aracridav.svua.proveedor.entity.Proveedor;
import cl.aracridav.svua.proveedor.repository.ProveedorRepository;
import cl.aracridav.svua.shared.enums.EstadoActivo;
import cl.aracridav.svua.shared.exception.BusinessException;
import cl.aracridav.svua.shared.mappers.GeneralMapper;
import cl.aracridav.svua.shared.util.SecurityUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ActivoServiceImpl implements ActivoService {

    private final ActivoRepository activoRepository;
    private final TipoActivoRepository tipoActivoRepository;
    private final UbicacionRepository ubicacionRepository;
    private final ProveedorRepository proveedorRepository;
    private final EmpresaRepository empresaRepository;
    private final OrdenMantenimientoRepository ordenRepository;

    private final HistorialEstadoActivoService historialService;
    private final DepreciacionService depreciacionService;

    private final GeneralMapper mapper;

    /*
     * =========================================
     * CREAR ACTIVO
     * =========================================
     */
    @Override
    public ActivoResponse crearActivo(ActivoCreateRequest req) {

        validarCodigoUnico(req.getCodigoInterno());

        Activo activo = construirActivo(req);

        Activo guardado = activoRepository.save(activo);

        procesarDepreciacion(guardado);

        return mapper.mapActivoResponse(guardado);
    }

    /*
     * =========================================
     * ACTUALIZAR ACTIVO
     * =========================================
     */
    @Override
    @Transactional
    public ActivoResponse actualizarActivo(Long activoId, ActivoUpdateRequest request) {

        Long empresaId = SecurityUtils.getEmpresaId();

        Activo activo = activoRepository.findById(activoId)
                .orElseThrow(() -> new BusinessException("Activo no encontrado"));

        // 🔐 Validar empresa (multi-tenant)
        if (!activo.getEmpresa().getId().equals(empresaId)) {
            throw new BusinessException("No pertenece a esta empresa");
        }

        // 🔁 Validar código único si cambia
        if (request.getCodigoInterno() != null &&
            !request.getCodigoInterno().equals(activo.getCodigoInterno()) &&
            activoRepository.existsByCodigoInterno(request.getCodigoInterno())) {

            throw new BusinessException("El código interno ya existe");
        }

        // 🔄 Actualización parcial
        if (request.getCodigoInterno() != null) {
            activo.setCodigoInterno(request.getCodigoInterno());
        }

        if (request.getNombre() != null) {
            activo.setNombre(request.getNombre());
        }

        if (request.getDescripcion() != null) {
            activo.setDescripcion(request.getDescripcion());
        }

        if (request.getMarca() != null) {
            activo.setMarca(request.getMarca());
        }

        if (request.getModelo() != null) {
            activo.setModelo(request.getModelo());
        }

        if (request.getNumeroSerie() != null) {
            activo.setNumeroSerie(request.getNumeroSerie());
        }

        if (request.getFechaAdquisicion() != null) {
            activo.setFechaAdquisicion(request.getFechaAdquisicion());
        }

        if (request.getValorAdquisicion() != null) {
            activo.setValorAdquisicion(request.getValorAdquisicion());
        }

        if (request.getValorResidual() != null) {
            activo.setValorResidual(request.getValorResidual());
        }

        if (request.getVidaUtilMeses() != null) {
            activo.setVidaUtilMeses(request.getVidaUtilMeses());
        }

        // 🔗 Relaciones
        if (request.getTipoActivoId() != null) {
            TipoActivo tipoActivo = tipoActivoRepository.findById(request.getTipoActivoId())
                    .orElseThrow(() -> new BusinessException("Tipo de activo no existe"));
            activo.setTipoActivo(tipoActivo);
        }

        if (request.getUbicacionId() != null) {
            Ubicacion ubicacion = ubicacionRepository.findById(request.getUbicacionId())
                    .orElseThrow(() -> new BusinessException("Ubicación no existe"));
            activo.setUbicacion(ubicacion);
        }

        if (request.getProveedorId() != null) {
            Proveedor proveedor = proveedorRepository.findById(request.getProveedorId())
                    .orElseThrow(() -> new BusinessException("Proveedor no existe"));
            activo.setProveedor(proveedor);
        }

        // 🔥 Cambio de estado con historial
        if (request.getEstadoActual() != null &&
            request.getEstadoActual() != activo.getEstadoActual()) {

            activo.setEstadoActual(request.getEstadoActual());

            historialService.registrarCambioEstado(
                    activo.getId(),
                    request.getEstadoActual(),
                    "Actualización manual de estado"
            );
        }

        if (request.getCuentaContable() != null) {
            activo.setCuentaContable(request.getCuentaContable());
        }

        Activo actualizado = activoRepository.save(activo);

        return mapper.mapActivoResponse(actualizado);
    }

    /*
     * =========================================
     * LISTAR
     * =========================================
     */
    @Override
    public Page<ActivoResponse> mostrarActivos(Pageable pageable) {
        return activoRepository.findAll(pageable)
                .map(mapper::mapActivoResponse);
    }

    /*
     * =========================================
     * ESTADOS
     * =========================================
     */
    @Override
    @Transactional
    public void darDeBaja(Long activoId, DarDeBajaActivoRequest request) {

        Long empresaId = SecurityUtils.getEmpresaId();

        Activo activo = activoRepository.findById(activoId)
                .orElseThrow(() -> new BusinessException("Activo no encontrado"));

        // 🔐 Validación multi-tenant
        if (!activo.getEmpresa().getId().equals(empresaId)) {
            throw new BusinessException("No pertenece a esta empresa");
        }

        // 🚫 Evitar doble baja
        if (activo.getEstadoActual() == EstadoActivo.BAJA) {
            throw new BusinessException("El activo ya está dado de baja");
        }

        // 🔄 Actualizar estado
        activo.setEstadoActual(EstadoActivo.BAJA);
        activo.setFechaBaja(LocalDate.now());
        activo.setMotivoBaja(request.getMotivo());

        activoRepository.save(activo);

        // 📜 Historial (reutilizando tu servicio)
        historialService.registrarCambioEstado(
                activo.getId(),
                EstadoActivo.BAJA,
                request.getMotivo()
        );
    }

    @Override
    public void actualizarEstado(Long id, EstadoActivo nuevoEstado) {

        Activo activo = obtenerActivo(id);

        activo.setEstadoActual(nuevoEstado);

        activoRepository.save(activo);

        historialService.registrarCambioEstado(
                activo.getId(),
                nuevoEstado,
                "Cambio automático de estado"
        );
    }

    /*
     * =========================================
     * RIESGO
     * =========================================
     */
    public double calcularRiesgo(Long activoId) {

        List<OrdenMantenimiento> ordenes =
                ordenRepository.findByActivoIdOrderByFechaProgramadaDesc(activoId);

        if (ordenes.isEmpty()) return 0;

        List<OrdenMantenimiento> recientes = limitarOrdenesRecientes(ordenes, 10);

        long dias = calcularDiasAnalisis(recientes);

        double frecuencia = calcularFrecuencia(recientes.size(), dias);

        return Math.round(normalizarRiesgo(frecuencia));
    }

    public String nivelRiesgo(double riesgo) {
        if (riesgo < 30) return "BAJO";
        if (riesgo < 70) return "MEDIO";
        return "ALTO";
    }

    /*
     * =========================================
     * HELPERS CREACIÓN
     * =========================================
     */
    private Activo construirActivo(ActivoCreateRequest req) {

        Empresa empresa = obtenerEmpresaActual();

        Activo activo = new Activo();

        activo.setCodigoInterno(req.getCodigoInterno());
        activo.setNombre(req.getNombre());
        activo.setDescripcion(req.getDescripcion());
        activo.setTipoActivo(obtenerTipoActivo(req.getTipoActivoId()));
        activo.setMarca(req.getMarca());
        activo.setModelo(req.getModelo());
        activo.setNumeroSerie(req.getNumeroSerie());
        activo.setFechaAdquisicion(req.getFechaAdquisicion());
        activo.setValorAdquisicion(req.getValorAdquisicion());
        activo.setValorResidual(req.getValorResidual());
        activo.setVidaUtilMeses(req.getVidaUtilMeses());
        activo.setEstadoActual(EstadoActivo.OPERATIVO);
        activo.setUbicacion(obtenerUbicacion(req.getUbicacionId()));
        activo.setProveedor(obtenerProveedor(req.getProveedorId()));
        activo.setCuentaContable(req.getCuentaContable());
        activo.setEmpresa(empresa);

        return activo;
    }

    private void procesarDepreciacion(Activo activo) {
        depreciacionService.guardarDepreciacion(activo);
        depreciacionService.calcularYGuardarDepreciacionMensual(activo);
    }

    /*
     * =========================================
     * HELPERS RIESGO
     * =========================================
     */
    private List<OrdenMantenimiento> limitarOrdenesRecientes(List<OrdenMantenimiento> ordenes, int limite) {
        return ordenes.stream().limit(limite).toList();
    }

    private long calcularDiasAnalisis(List<OrdenMantenimiento> ordenes) {

        LocalDateTime primera = ordenes.get(ordenes.size() - 1).getFechaProgramada();
        long dias = ChronoUnit.DAYS.between(primera, LocalDateTime.now());

        return dias == 0 ? 1 : dias;
    }

    private double calcularFrecuencia(int total, long dias) {
        return (double) total / dias;
    }

    private double normalizarRiesgo(double frecuencia) {
        return Math.min(frecuencia * 100, 100);
    }

    /*
     * =========================================
     * VALIDACIONES
     * =========================================
     */
    private void validarCodigoUnico(String codigo) {
        if (activoRepository.existsByCodigoInterno(codigo)) {
            throw new BusinessException("El código interno ya existe");
        }
    }

    /*
     * =========================================
     * FINDERS
     * =========================================
     */
    private Activo obtenerActivo(Long id) {
        return activoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Activo no existe"));
    }

    private TipoActivo obtenerTipoActivo(Long id) {
        return tipoActivoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Tipo de activo no existe"));
    }

    private Ubicacion obtenerUbicacion(Long id) {
        return ubicacionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Ubicación no existe"));
    }

    private Proveedor obtenerProveedor(Long id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Proveedor no existe"));
    }

    private Empresa obtenerEmpresaActual() {
        return empresaRepository.findById(SecurityUtils.getEmpresaId())
                .orElseThrow(() -> new BusinessException("Empresa no encontrada"));
    }
}