package cl.aracridav.svua.inventario.activo.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import cl.aracridav.svua.depreciacion.service.DepreciacionService;
import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.empresa.repository.EmpresaRepository;
import cl.aracridav.svua.inventario.activo.dto.request.ActivoCreateRequest;
import cl.aracridav.svua.inventario.activo.dto.request.ActivoUpdateRequest;
import cl.aracridav.svua.inventario.activo.dto.request.DarDeBajaActivoRequest;
import cl.aracridav.svua.inventario.activo.dto.response.ActivoEscaneoResponse;
import cl.aracridav.svua.inventario.activo.dto.response.ActivoResponse;
import cl.aracridav.svua.inventario.activo.entity.Activo;
import cl.aracridav.svua.inventario.activo.repository.ActivoRepository;
import cl.aracridav.svua.inventario.activo.util.ActivoCodigoGenerador;
import cl.aracridav.svua.inventario.historial.service.HistorialEstadoActivoService;
import cl.aracridav.svua.inventario.tipoactivo.entity.TipoActivo;
import cl.aracridav.svua.inventario.tipoactivo.repository.TipoActivoRepository;
import cl.aracridav.svua.inventario.ubicacion.entity.Ubicacion;
import cl.aracridav.svua.inventario.ubicacion.repository.UbicacionRepository;
import cl.aracridav.svua.mantenimiento.orden.dto.response.OrdenMantenimientoResponse;
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
        validarVidaUtilMeses(req.getVidaUtilMeses());

        Activo activo = construirActivo(req);

        Activo guardado = activoRepository.save(activo);

        procesarDepreciacion(guardado);

        historialService.registrarCambioEstado(
                activo.getId(), EstadoActivo.OPERATIVO, null, "Creación de Activo", null
            );

        return ocultarCodigosSiNoCorresponde(mapper.mapActivoResponse(guardado));
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

        // 🔒 El codigo interno ya no se puede modificar desde el
        // mantenedor (queda fijo desde la creacion, junto con el QR/EAN13
        // generados a partir de el) - ActivoUpdateRequest ni siquiera trae
        // ese campo, asi que no hay nada que aplicar aqui.

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
            // 🔒 Evita dejar el activo con una vida útil en 0 (o negativa),
            // que más adelante rompería el cálculo de depreciación con una
            // división por cero.
            validarVidaUtilMeses(request.getVidaUtilMeses());
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
                    request.getEstadoActual(),
                    "Actualización manual de estado",
                    null
            );
        }

        if (request.getCuentaContable() != null) {
            activo.setCuentaContable(request.getCuentaContable());
        }

        Activo actualizado = activoRepository.save(activo);

        return ocultarCodigosSiNoCorresponde(mapper.mapActivoResponse(actualizado));
    }

    /*
     * =========================================
     * LISTAR
     * =========================================
     */
    @Override
    public Page<ActivoResponse> mostrarActivos(Pageable pageable, Long empresaId, String busqueda) {

        // 🔒 Usuarios no SUPER_ADMIN siempre ven solo su propia empresa, sin
        // importar lo que llegue en empresaId (mismo criterio que antes).
        // 🔥 SUPER_ADMIN puede ver todas las empresas o filtrar por una.
        // 🔍 busqueda (codigo interno o nombre) es opcional y aplica en
        // ambos casos.
        Long empresaIdEfectivo = esSuperAdmin() ? empresaId : SecurityUtils.getEmpresaId();

        return activoRepository.buscarActivos(empresaIdEfectivo, busqueda, pageable)
                .map(mapper::mapActivoResponse)
                .map(this::ocultarCodigosSiNoCorresponde);
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
                EstadoActivo.OPERATIVO,
                request.getMotivo(),
                null
        );
    }

    @Override
    public void actualizarEstado(Long id, EstadoActivo nuevoEstado) {

        Activo activo = obtenerActivo(id);

        EstadoActivo viejoEstado = activo.getEstadoActual();

        activo.setEstadoActual(nuevoEstado);

        activoRepository.save(activo);

        historialService.registrarCambioEstado(
                activo.getId(),
                nuevoEstado,
                viejoEstado,
                "Cambio automático de estado",
                null
        );
    }

    /*
     * =========================================
     * RIESGO
     * =========================================
     */
    @Override
    public double calcularRiesgo(Long activoId) {

        List<OrdenMantenimiento> ordenes =
                ordenRepository.findByActivoIdOrderByFechaProgramadaDesc(activoId);

        if (ordenes.isEmpty()) return 0;

        List<OrdenMantenimiento> recientes = limitarOrdenesRecientes(ordenes, 10);

        long dias = calcularDiasAnalisis(recientes);

        double frecuencia = calcularFrecuencia(recientes.size(), dias);

        return Math.round(normalizarRiesgo(frecuencia));
    }

    @Override
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
        activo.setCuentaContable(req.getCuentaContable() != null ? req.getCuentaContable() : "0");

        // 🔳 Codigo QR y EAN13: se generan automaticamente a partir del
        // codigo interno, no los ingresa el usuario.
        activo.setCodigoQr(ActivoCodigoGenerador.generarCodigoQr(req.getCodigoInterno()));
        activo.setCodigoEan13(ActivoCodigoGenerador.generarCodigoEan13(req.getCodigoInterno()));

        activo.setFechaCreacion(LocalDateTime.now());
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

    // 🔒 La vida útil (en meses) es el denominador del cálculo de
    // depreciación mensual (ver DepreciacionServiceImpl). Sin esta
    // validación, un valor en null, 0 o negativo llegaba hasta ese cálculo
    // y rompía la creación/actualización del activo con una
    // ArithmeticException (división por cero) o un NullPointerException,
    // en el caso de "crear" incluso después de haber guardado el activo.
    private void validarVidaUtilMeses(Integer vidaUtilMeses) {
        if (vidaUtilMeses == null || vidaUtilMeses <= 0) {
            vidaUtilMeses = 1;
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

    /*
     * =========================================
     * ESCANEO QR / EAN13
     * =========================================
     */
    @Override
    public ActivoEscaneoResponse buscarPorCodigoEscaneado(String codigo) {

        // 🔒 Escaneo de QR/EAN13 disponible para SUPER_ADMIN y para toda
        // empresa que tenga habilitado al menos uno de los dos codigos
        // (Empresa.codigoQrHabilitado / codigoEan13Habilitado), sin importar
        // el rol dentro de esa empresa.
        boolean tieneAlgunCodigoHabilitado =
                SecurityUtils.tieneCodigoQrHabilitado() || SecurityUtils.tieneCodigoEan13Habilitado();

        if (!esSuperAdmin() && !tieneAlgunCodigoHabilitado) {
            throw new BusinessException("El escaneo de activos no está disponible para tu empresa");
        }

        if (codigo == null || codigo.isBlank()) {
            throw new BusinessException("El código escaneado no puede estar vacío");
        }

        String codigoLimpio = codigo.trim();

        // 🔒 SUPER_ADMIN puede escanear activos de cualquier empresa; el
        // resto de los roles solo activos de su propia empresa (mismo
        // criterio que mostrarActivos).
        Activo activo = esSuperAdmin()
                ? buscarActivoPorCodigoGlobal(codigoLimpio)
                : buscarActivoPorCodigoEnEmpresa(codigoLimpio, SecurityUtils.getEmpresaId());

        List<OrdenMantenimientoResponse> mantenciones = ordenRepository
                .findByActivoIdOrderByFechaProgramadaDesc(activo.getId())
                .stream()
                .map(mapper::mapOrdenMantenimientoResponse)
                .toList();

        return ActivoEscaneoResponse.builder()
                .activo(ocultarCodigosSiNoCorresponde(mapper.mapActivoResponse(activo)))
                .mantenciones(mantenciones)
                .build();
    }

    // 🔳 El QR guarda el codigoInterno tal cual y el EAN13 se deriva de el
    // (ver ActivoCodigoGenerador): probamos ambos, sin saber de antemano
    // cual de los dos se leyo.
    private Activo buscarActivoPorCodigoEnEmpresa(String codigo, Long empresaId) {
        return activoRepository.findByCodigoInternoAndEmpresaId(codigo, empresaId)
                .or(() -> activoRepository.findByCodigoEan13AndEmpresaId(codigo, empresaId))
                .orElseThrow(() -> new BusinessException("No se encontró ningún activo con ese código"));
    }

    private Activo buscarActivoPorCodigoGlobal(String codigo) {
        return activoRepository.findByCodigoInterno(codigo)
                .or(() -> activoRepository.findByCodigoEan13(codigo))
                .orElseThrow(() -> new BusinessException("No se encontró ningún activo con ese código"));
    }

    private boolean esSuperAdmin() {
        return SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
    }

    // 🔒 Cada codigo se muestra de forma independiente segun lo que la
    // empresa tenga habilitado (Empresa.codigoQrHabilitado /
    // codigoEan13Habilitado); SUPER_ADMIN siempre ve ambos. El activo los
    // sigue generando y guardando igual sin importar la configuracion de
    // su empresa, simplemente no se exponen si corresponde.
    private ActivoResponse ocultarCodigosSiNoCorresponde(ActivoResponse response) {

        if (esSuperAdmin()) {
            return response;
        }

        if (!SecurityUtils.tieneCodigoQrHabilitado()) {
            response.setCodigoQr(null);
        }

        if (!SecurityUtils.tieneCodigoEan13Habilitado()) {
            response.setCodigoEan13(null);
        }

        return response;
    }
}
