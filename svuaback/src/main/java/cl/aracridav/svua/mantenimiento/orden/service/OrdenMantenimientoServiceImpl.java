package cl.aracridav.svua.mantenimiento.orden.service;

import java.math.BigDecimal;
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
import cl.aracridav.svua.mantenimiento.orden.dto.response.OrdenMantenimientoResponse;
import cl.aracridav.svua.mantenimiento.orden.entity.EstadoOrden;
import cl.aracridav.svua.mantenimiento.orden.entity.OrdenMantenimiento;
import cl.aracridav.svua.mantenimiento.orden.repository.OrdenMantenimientoRepository;
import cl.aracridav.svua.mantenimiento.plan.entity.PlanMantenimiento;
import cl.aracridav.svua.mantenimiento.plan.repository.PlanMantenimientoRepository;
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
    private final ActivoRepository activoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PlanMantenimientoRepository planRepository;
    private final EmpresaRepository empresaRepository;
    private final GeneralMapper generalMapper;

    public OrdenMantenimiento ejecutarOrden(Long idOrden) {

        OrdenMantenimiento orden = ordenRepository.findById(idOrden)
            .orElseThrow(() ->
                new BusinessException("Orden no existe")
            );

        Activo activo = orden.getActivo();

        if ("BAJA".equals(activo.getEstadoActual().toString())) {
            throw new BusinessException(
                "No se puede mantener un activo dado de baja"
            );
        }

        orden.setEstado(EstadoOrden.EN_EJECUCION);
        orden.setFechaEjecucion(LocalDateTime.now());

        return orden;
    }

    /*
     * =========================================
     * CREAR ORDEN MANUAL
     * =========================================
     */
    public OrdenMantenimientoResponse crearOrden(OrdenMantenimientoRequest request) {

        Long empresaId = SecurityUtils.getEmpresaId();

        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new BusinessException("Empresa no encontrada"));

        Activo activo = activoRepository.findById(request.getActivoId())
                .orElseThrow(() -> new BusinessException("Activo no existe"));

        Usuario usuario = usuarioRepository.findById(request.getUsuarioId())
                .orElseThrow(() -> new BusinessException("Usuario no existe"));

        PlanMantenimiento plan = planRepository.findById(request.getPlanMantenimientoId())
                .orElseThrow(() -> new BusinessException("Plan mantenimiento no encontrado"));

        // 🔥 Regla: no permitir otra orden pendiente
        boolean existePendiente = ordenRepository
                .existsByActivoIdAndEstado(request.getActivoId(), EstadoOrden.PENDIENTE);

        if (existePendiente) {
            throw new BusinessException("Ya existe una orden pendiente para este activo");
        }

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

        OrdenMantenimiento ordenMantenimientoGuardada = ordenRepository.save(orden);

        return generalMapper.mapOrdenMantenimientoResponse(ordenMantenimientoGuardada);
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
    public OrdenMantenimiento cerrarOrden(
            Long ordenId,
            BigDecimal costo,
            String observacionesFinales
    ) {

        OrdenMantenimiento orden = ordenRepository.findById(ordenId)
                .orElseThrow(() -> new BusinessException("Orden no existe"));

        if (orden.getEstado() != EstadoOrden.PENDIENTE) {
            throw new BusinessException("Solo se pueden cerrar órdenes pendientes");
        }

        orden.setEstado(EstadoOrden.COMPLETADA);
        orden.setFechaEjecucion(LocalDateTime.now());
        orden.setCosto(costo);
        orden.setObservaciones(observacionesFinales);

        return ordenRepository.save(orden);
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


}
