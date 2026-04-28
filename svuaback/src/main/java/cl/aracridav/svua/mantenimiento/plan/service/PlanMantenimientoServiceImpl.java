package cl.aracridav.svua.mantenimiento.plan.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.empresa.repository.EmpresaRepository;
import cl.aracridav.svua.inventario.activo.entity.Activo;
import cl.aracridav.svua.inventario.activo.repository.ActivoRepository;
import cl.aracridav.svua.mantenimiento.orden.entity.EstadoOrden;
import cl.aracridav.svua.mantenimiento.orden.entity.OrdenMantenimiento;
import cl.aracridav.svua.mantenimiento.orden.repository.OrdenMantenimientoRepository;
import cl.aracridav.svua.mantenimiento.plan.dto.request.PlanMantenimientoCreateRequest;
import cl.aracridav.svua.mantenimiento.plan.dto.response.PlanMantenimientoReponse;
import cl.aracridav.svua.mantenimiento.plan.entity.PlanMantenimiento;
import cl.aracridav.svua.mantenimiento.plan.repository.PlanMantenimientoRepository;
import cl.aracridav.svua.shared.exception.BusinessException;
import cl.aracridav.svua.shared.mappers.GeneralMapper;
import cl.aracridav.svua.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
@Transactional
public class PlanMantenimientoServiceImpl implements PlanMantenimientoService {

    private final PlanMantenimientoRepository repository;
    private final ActivoRepository activoRepository;
    private final OrdenMantenimientoRepository ordenRepository;
    private final EmpresaRepository empresaRepository;
    private final GeneralMapper mapper;

    /*
     * =========================================
     * CREAR
     * =========================================
     */
    @Override
    public PlanMantenimientoReponse crear(PlanMantenimientoCreateRequest request) {

        Empresa empresa = obtenerEmpresaActual();
        Activo activo = obtenerActivo(request.getActivoId());

        PlanMantenimiento plan = construirPlan(request, empresa, activo);

        return mapper.mapPlanMantenimientotoResponse(repository.save(plan));
    }

    /*
     * =========================================
     * ACTUALIZAR
     * =========================================
     */
    @Override
    public PlanMantenimientoReponse actualizar(Long id, PlanMantenimientoCreateRequest request) {

        PlanMantenimiento plan = obtenerPlan(id);

        plan.setTipoMantenimiento(request.getTipoMantenimiento());
        plan.setFrecuenciaDias(request.getFrecuenciaDias());
        plan.setDescripcion(request.getDescripcion());

        return mapper.mapPlanMantenimientotoResponse(repository.save(plan));
    }

    /*
     * =========================================
     * DESACTIVAR
     * =========================================
     */
    @Override
    public void desactivar(Long id) {

        PlanMantenimiento plan = obtenerPlan(id);
        plan.setEstaActivo(false);

        repository.save(plan);
    }

    /*
     * =========================================
     * LISTAR
     * =========================================
     */
    @Override
    public List<PlanMantenimientoReponse> listar() {

        return repository.findByEmpresaId(SecurityUtils.getEmpresaId())
                .stream()
                .map(mapper::mapPlanMantenimientotoResponse)
                .toList();
    }

    /*
     * =========================================
     * PLANES VENCIDOS (ENTIDAD)
     * =========================================
     */
    @Transactional(readOnly = true)
    public List<PlanMantenimientoReponse> obtenerPlanesVencidosEntity() {

        return repository
                .findByEstaActivoTrueAndProximaEjecucionLessThanEqual(LocalDateTime.now());
    }

    /*
     * =========================================
     * SCHEDULER
     * =========================================
     */
    @Override
    @Transactional
    public void procesarPlanesVencidos() {

        List<PlanMantenimientoReponse> planes = obtenerPlanesVencidosEntity();

        for (PlanMantenimientoReponse plan : planes) {

            if (existeOrdenPendiente(plan)) continue;

            crearOrdenPreventiva(plan);
            actualizarPlan(plan);
        }
    }

    /*
     * =========================================
     * CORE
     * =========================================
     */

    private boolean existeOrdenPendiente(PlanMantenimientoReponse plan) {

        return ordenRepository.existsByActivoIdAndEstado(
                plan.getActivoId(),
                EstadoOrden.PENDIENTE
        );
    }

    private void crearOrdenPreventiva(PlanMantenimientoReponse plan) {

        OrdenMantenimiento orden = new OrdenMantenimiento();
        Activo activo = obtenerActivo(plan.getActivoId());

        orden.setActivo(activo);
        orden.setFechaProgramada(LocalDateTime.now());
        orden.setTipoMantenimiento(plan.getTipoMantenimiento());
        orden.setEstado(EstadoOrden.PENDIENTE);
        orden.setCosto(BigDecimal.ZERO);

        ordenRepository.save(orden);
    }

    private void actualizarPlan(PlanMantenimientoReponse plan) {

        PlanMantenimiento planAct = obtenerPlan(plan.getId());

        LocalDateTime ahora = LocalDateTime.now();

        planAct.setUltimaEjecucion(ahora);
        planAct.setProximaEjecucion(ahora.plusDays(planAct.getFrecuenciaDias()));

        repository.save(planAct);
    }

    /*
     * =========================================
     * BUILDER
     * =========================================
     */
    private PlanMantenimiento construirPlan(
            PlanMantenimientoCreateRequest request,
            Empresa empresa,
            Activo activo) {

        PlanMantenimiento plan = new PlanMantenimiento();

        plan.setTipoMantenimiento(request.getTipoMantenimiento());
        plan.setFrecuenciaDias(request.getFrecuenciaDias());
        plan.setDescripcion(request.getDescripcion());
        plan.setEstaActivo(true);
        plan.setUltimaEjecucion(null);
        plan.setProximaEjecucion(LocalDateTime.now().plusDays(request.getFrecuenciaDias()));
        plan.setEmpresa(empresa);
        plan.setActivo(activo);

        return plan;
    }

    /*
     * =========================================
     * HELPERS
     * =========================================
     */

    private Empresa obtenerEmpresaActual() {
        return empresaRepository.findById(SecurityUtils.getEmpresaId())
                .orElseThrow(() -> new BusinessException("Empresa no encontrada"));
    }

    private Activo obtenerActivo(Long id) {
        return activoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Activo no encontrado"));
    }

    private PlanMantenimiento obtenerPlan(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException("Plan no encontrado"));
    }
}