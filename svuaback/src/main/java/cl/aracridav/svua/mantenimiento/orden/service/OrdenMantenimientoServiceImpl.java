package cl.aracridav.svua.mantenimiento.orden.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.empresa.repository.EmpresaRepository;
import cl.aracridav.svua.inventario.activo.entity.Activo;
import cl.aracridav.svua.inventario.activo.repository.ActivoRepository;
import cl.aracridav.svua.mantenimiento.orden.dto.request.OrdenMantenimientoRequest;
import cl.aracridav.svua.mantenimiento.orden.dto.response.OrdenEjecucionResponse;
import cl.aracridav.svua.mantenimiento.orden.dto.response.OrdenMantenimientoResponse;
import cl.aracridav.svua.mantenimiento.orden.entity.EstadoOrden;
import cl.aracridav.svua.mantenimiento.orden.entity.OrdenMantenimiento;
import cl.aracridav.svua.mantenimiento.orden.entity.OrdenReprogramacion;
import cl.aracridav.svua.mantenimiento.orden.repository.OrdenMantenimientoRepository;
import cl.aracridav.svua.mantenimiento.orden.repository.OrdenReprogramacionRepository;
import cl.aracridav.svua.mantenimiento.plan.entity.PlanMantenimiento;
import cl.aracridav.svua.mantenimiento.plan.repository.PlanMantenimientoRepository;
import cl.aracridav.svua.shared.enums.EstadoActivo;
import cl.aracridav.svua.shared.exception.BusinessException;
import cl.aracridav.svua.shared.mappers.GeneralMapper;
import cl.aracridav.svua.shared.util.SecurityUtils;
import cl.aracridav.svua.usuario.entity.Usuario;
import cl.aracridav.svua.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class OrdenMantenimientoServiceImpl implements OrdenMantenimientoService {

    private final OrdenMantenimientoRepository ordenRepository;
    private final OrdenReprogramacionRepository ordenReprogramacionRepository;
    private final ActivoRepository activoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PlanMantenimientoRepository planRepository;
    private final EmpresaRepository empresaRepository;
    private final GeneralMapper generalMapper;

    /*
     * =========================================
     * EJECUTAR ORDEN
     * =========================================
     */
    public OrdenEjecucionResponse ejecutarOrden(Long idOrden) {

        OrdenMantenimiento orden = ordenRepository.findById(idOrden)
            .orElseThrow(() -> new BusinessException("Orden no existe"));

        return cambiarEstado(orden, EstadoOrden.EN_EJECUCION);
    }

    /*
     * =========================================
     * CREAR ORDEN MANUAL
     * =========================================
     */
    public OrdenMantenimientoResponse crearOrden(OrdenMantenimientoRequest request) {

        Empresa empresa = obtenerEmpresaActual();
        Activo activo = obtenerActivo(request.getActivoId());
        Usuario usuario = obtenerUsuario(request.getUsuarioId());
        PlanMantenimiento plan = obtenerPlan(request.getPlanMantenimientoId());

        validarNoExisteOrdenPendiente(activo.getId());

        OrdenMantenimiento orden = construirOrden(request, empresa, activo, usuario, plan);

        OrdenMantenimiento guardada = ordenRepository.save(orden);


        return generalMapper.mapOrdenMantenimientoResponse(guardada);
    }

    /*
     * =========================================
     * GENERAR ORDEN DESDE PLAN PREVENTIVO
     * =========================================
     */
    public OrdenMantenimiento generarDesdePlan(Long planId, Long usuarioId) {

        PlanMantenimiento plan = planRepository.findById(planId)
                .orElseThrow(() -> new BusinessException("Plan no existe"));

        if (!plan.getEstaActivo()) {
            throw new BusinessException("El plan no está activo");
        }

        Long activoId = plan.getActivo().getId();

        boolean existePendiente = ordenRepository
                .existsByActivoIdAndEstado(activoId, EstadoOrden.PENDIENTE);

        if (existePendiente) {
            throw new BusinessException("Ya existe orden pendiente para este activo");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new BusinessException("Usuario no existe"));

        OrdenMantenimiento orden = new OrdenMantenimiento();
        orden.setActivo(plan.getActivo());
        orden.setUsuario(usuario);
        orden.setTipoMantenimiento(plan.getTipoMantenimiento());
        orden.setFechaProgramada(plan.getProximaEjecucion());
        orden.setEstado(EstadoOrden.PENDIENTE);
        orden.setPlanMantenimiento(plan);

        return ordenRepository.save(orden);
    }

    /*
     * =========================================
     * CERRAR ORDEN
     * =========================================
     */
    public OrdenEjecucionResponse cerrarOrden(Long ordenId, BigDecimal costo, String observacionesFinales) {

        OrdenMantenimiento orden = ordenRepository.findById(ordenId)
            .orElseThrow(() -> new BusinessException("Orden no existe"));

        orden.setCosto(costo);
        orden.setObservaciones(observacionesFinales);

        return cambiarEstado(orden, EstadoOrden.COMPLETADA);
    }

    /*
     * =========================================
     * DETENER ORDEN
     * =========================================
     */
    public OrdenEjecucionResponse detenerOrden(Long idOrden) {

        OrdenMantenimiento orden = ordenRepository.findById(idOrden)
            .orElseThrow(() -> new BusinessException("Orden no existe"));

        return cambiarEstado(orden, EstadoOrden.COMPLETADA);
    }

    /*
     * =========================================
     * CANCELAR ORDEN
     * =========================================
     */
    public OrdenMantenimiento cancelarOrden(Long ordenId, String motivo) {

        OrdenMantenimiento orden = ordenRepository.findById(ordenId)
                .orElseThrow(() -> new BusinessException("Orden no existe"));

        if (orden.getEstado() == EstadoOrden.COMPLETADA) {
            throw new BusinessException("No se puede cancelar una orden cerrada");
        }

        orden.setEstado(EstadoOrden.CANCELADA);
        orden.setObservaciones(motivo);

        return ordenRepository.save(orden);
    }

    /*
     * =========================================
     * LISTAR ÓRDENES VENCIDAS
     * =========================================
     */
    @Transactional(readOnly = true)
    public List<OrdenMantenimiento> obtenerOrdenesVencidas() {
        return ordenRepository.findOrdenesVencidas(LocalDate.now());
    }

    /*
     * =========================================
     * LISTAR ÓRDENES EMPRESAS
     * =========================================
     */
    @Transactional(readOnly = true)
    public List<OrdenMantenimientoResponse> listarOrdenesEmpresa() {

        Long empresaId = SecurityUtils.getEmpresaId();

        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new BusinessException("Empresa no encontrada"));

        return ordenRepository.findByEmpresaId(empresa.getId())
                .stream()
                .map(generalMapper::mapOrdenMantenimientoResponse)
                .collect(Collectors.toList());
    }

    /*
     * =========================================
     * ACTUALIZAR ORDEN MANUAL
     * =========================================
     */
    public OrdenMantenimientoResponse actualizarOrden(Long ordenId, OrdenMantenimientoRequest request) {

        OrdenMantenimiento orden = ordenRepository.findById(ordenId)
                .orElseThrow(() -> new BusinessException("Orden no existe"));


        orden.setTitulo(request.getTitulo());
        orden.setFechaProgramada(request.getFechaProgramada());
        

        OrdenMantenimiento ordenMantenimientoGuardada = ordenRepository.save(orden);

        return generalMapper.mapOrdenMantenimientoResponse(ordenMantenimientoGuardada);
    }

    /*
     * =========================================
     * REPROGRAMAR ORDEN
     * =========================================
     */
    public OrdenMantenimientoResponse reprogramarOrden(Long ordenId, LocalDateTime nuevaFecha, String motivo) {

        Long empresaId = SecurityUtils.getEmpresaId();
        Long usuarioId = SecurityUtils.getUsuarioId();
        
        OrdenMantenimiento orden = ordenRepository.findById(ordenId)
            .orElseThrow(() -> new BusinessException("Orden no existe"));

        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new BusinessException("Empresa no encontrada"));

        validarEstadoReprogramacion(orden.getEstado());
        validarFechaReprogramacion(nuevaFecha);

        // 🔥 Guardar historial
        OrdenReprogramacion r = new OrdenReprogramacion();
        r.setOrden(orden);
        r.setFechaAnterior(orden.getFechaProgramada());
        r.setFechaNueva(nuevaFecha);
        r.setUsuario(usuario);
        r.setMotivo(motivo);
        r.setEmpresa(empresa);

        ordenReprogramacionRepository.save(r);

        orden.setFechaProgramada(nuevaFecha);

        OrdenMantenimiento ordenMantenimientoGuardada = ordenRepository.save(orden);

        return generalMapper.mapOrdenMantenimientoResponse(ordenMantenimientoGuardada);
    }

    private OrdenEjecucionResponse cambiarEstado(OrdenMantenimiento orden, EstadoOrden nuevoEstado) {

        EstadoOrden actual = orden.getEstado();

        EstadoActivo estadoActivo = EstadoActivo.FUERA_SERVICIO;

        if (!actual.puedePasarA(nuevoEstado)) {
            throw new BusinessException(
                "Transición inválida: " + actual + " → " + nuevoEstado
            );
        }

        aplicarReglas(orden, nuevoEstado);

        orden.setEstado(nuevoEstado);

        OrdenMantenimiento guardada = ordenRepository.save(orden);

        Activo activo = obtenerActivoDeOrden(guardada.getId());

        if (nuevoEstado == EstadoOrden.COMPLETADA){
            estadoActivo = EstadoActivo.OPERATIVO;
        }

        activo.setEstadoActual(estadoActivo);

        activoRepository.save(activo);

        return generalMapper.mapOrdenEjecucionResponse(guardada);

    }

    private void aplicarReglas(OrdenMantenimiento orden, EstadoOrden nuevoEstado) {

        switch (nuevoEstado) {

            case EN_EJECUCION -> {

                if ("BAJA".equals(orden.getActivo().getEstadoActual().toString())) {
                    throw new BusinessException("Activo dado de baja");
                }

                orden.setFechaEjecucion(LocalDateTime.now());
                orden.setUsuarioEjecucion(getUsuarioActual());
            }

            case COMPLETADA -> {

                if (orden.getFechaEjecucion() == null) {
                    throw new BusinessException(
                        "No se puede completar sin haber iniciado la orden"
                    );
                }

                LocalDateTime ahora = LocalDateTime.now();

                orden.setFechaFinEjecucion(ahora);

                long duracion = Duration.between(
                    orden.getFechaEjecucion(),
                    ahora
                ).getSeconds();

                orden.setDuracionSegundos(duracion);
                orden.setUsuarioFinalizacion(getUsuarioActual());
            }

            case CANCELADA -> {

                LocalDateTime ahora = LocalDateTime.now();

                orden.setFechaFinEjecucion(ahora);

                if (orden.getFechaEjecucion() != null) {
                    long duracion = Duration.between(
                        orden.getFechaEjecucion(),
                        ahora
                    ).getSeconds();

                    orden.setDuracionSegundos(duracion);
                }

                orden.setUsuarioFinalizacion(getUsuarioActual());
            }

            default -> {}
        }
    }

    private Usuario getUsuarioActual() {
        Long usuarioId = SecurityUtils.getUsuarioId();

        return usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    private Empresa obtenerEmpresaActual() {
        Long empresaId = SecurityUtils.getEmpresaId();

        return empresaRepository.findById(empresaId)
            .orElseThrow(() -> new BusinessException("Empresa no encontrada"));
    }

    private Activo obtenerActivo(Long activoId) {
        return activoRepository.findById(activoId)
            .orElseThrow(() -> new BusinessException("Activo no existe"));
    }

    private Usuario obtenerUsuario(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new BusinessException("Usuario no existe"));
    }

    private PlanMantenimiento obtenerPlan(Long planId) {
        return planRepository.findById(planId)
            .orElseThrow(() -> new BusinessException("Plan mantenimiento no encontrado"));
    }

    public Activo obtenerActivoDeOrden(Long ordenId) {
    return ordenRepository.findActivoByOrdenId(ordenId)
        .orElseThrow(() -> new RuntimeException("Activo no encontrado"));
}

    private void validarNoExisteOrdenPendiente(Long activoId) {

        boolean existePendiente = ordenRepository
            .existsByActivoIdAndEstado(activoId, EstadoOrden.PENDIENTE);

        if (existePendiente) {
            throw new BusinessException(
                "Ya existe una orden pendiente para este activo"
            );
        }
    }

    private OrdenMantenimiento construirOrden(
        OrdenMantenimientoRequest request,
        Empresa empresa,
        Activo activo,
        Usuario usuario,
        PlanMantenimiento plan
    ) {

        OrdenMantenimiento orden = new OrdenMantenimiento();

        orden.setTitulo(request.getTitulo());
        orden.setFechaProgramada(request.getFechaProgramada());
        orden.setTipoMantenimiento(request.getTipoMantenimiento());
        orden.setEstado(EstadoOrden.PROGRAMADA);
        orden.setCosto(request.getCosto());
        orden.setObservaciones(request.getObservaciones());

        orden.setActivo(activo);
        orden.setUsuario(usuario);
        orden.setPlanMantenimiento(plan);
        orden.setEmpresa(empresa);

        return orden;
    }

    private void validarEstadoReprogramacion(EstadoOrden estado) {

        if (estado != EstadoOrden.PENDIENTE && estado != EstadoOrden.PROGRAMADA) {
            throw new BusinessException(
                "Solo se pueden reprogramar órdenes en estado PENDIENTE o PROGRAMADA"
            );
        }
    }

    private void validarFechaReprogramacion(LocalDateTime nuevaFecha) {

        if (nuevaFecha == null) {
            throw new BusinessException("La nueva fecha es obligatoria");
        }

        if (nuevaFecha.isBefore(LocalDateTime.now())) {
            throw new BusinessException(
                "La fecha de programación no puede ser inferior a la fecha actual"
            );
        }
    }


}
