package cl.aracridav.svua.mantenimiento.plan.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
@Transactional
public class PlanMantenimientoServiceImpl implements PlanMantenimientoService {

    private final PlanMantenimientoRepository repository;
    private final ActivoRepository activoRepository;
    private final OrdenMantenimientoRepository ordenRepository;
    private final EmpresaRepository empresaRepository;
    private final GeneralMapper generalMapper;

    @Override
    public PlanMantenimientoReponse crear(PlanMantenimientoCreateRequest request) {

        Long empresaId = SecurityUtils.getEmpresaId();

        Activo activo = activoRepository.findById(request.getActivoId())
                .orElseThrow(() -> new BusinessException("Activo no encontrado"));

        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new BusinessException("Empresa no encontrada"));

        PlanMantenimiento plan = new PlanMantenimiento();
        plan.setTipoMantenimiento(request.getTipoMantenimiento());
        plan.setFrecuenciaDias(request.getFrecuenciaDias());
        plan.setDescripcion(request.getDescripcion());
        plan.setEstaActivo(true);
        plan.setUltimaEjecucion(null);
        plan.setProximaEjecucion(LocalDateTime.now().plusDays(request.getFrecuenciaDias()));
        plan.setEmpresa(empresa);
        plan.setActivo(activo);

        PlanMantenimiento planGuardado = repository.save(plan);

        return generalMapper.mapPlanMantenimientotoResponse(planGuardado);
    }

    @Override
    public PlanMantenimientoReponse actualizar(Long id, PlanMantenimientoCreateRequest request) {

        PlanMantenimiento plan = repository.findById(id)
                .orElseThrow(() -> new BusinessException("Plan no encontrado"));

        plan.setTipoMantenimiento(request.getTipoMantenimiento());
        plan.setFrecuenciaDias(request.getFrecuenciaDias());
        plan.setDescripcion(request.getDescripcion());

        PlanMantenimiento planGuardado = repository.save(plan);

        return generalMapper.mapPlanMantenimientotoResponse(planGuardado);
    }

    @Override
    public void desactivar(Long id) {

        PlanMantenimiento plan = repository.findById(id)
                .orElseThrow(() -> new BusinessException("Plan no encontrado"));

        plan.setEstaActivo(false);
        repository.save(plan);
    }

    public PlanMantenimientoReponse obtener(Long id) {

        PlanMantenimiento entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan no encontrado"));

        return generalMapper.mapPlanMantenimientotoResponse(entity);
    }

    public List<PlanMantenimientoReponse> listar() {

        Long empresaId = SecurityUtils.getEmpresaId();

        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new BusinessException("Empresa no encontrada"));

        return repository.findByEmpresaId(empresa.getId())
                .stream()
                .map(generalMapper::mapPlanMantenimientotoResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<PlanMantenimientoReponse> obtenerPlanesVencidos() {
        
        return repository.findByEstaActivoTrueAndProximaEjecucionLessThanEqual(LocalDate.now())
            .stream()
            .map(generalMapper::mapPlanMantenimientotoResponse)
            .toList();
    }
    
    /**
     * Método que será usado por un Scheduler
     */
    @Override
    public void procesarPlanesVencidos() {

        List<PlanMantenimientoReponse> planesVencidos = obtenerPlanesVencidos();

        for (PlanMantenimientoReponse plan : planesVencidos) {

            // 🔎 Verificar que no exista orden pendiente
            boolean existeOrdenPendiente = ordenRepository
                    .existsByActivoIdAndEstado(
                            plan.getActivoId(),
                            EstadoOrden.PENDIENTE
                    );

            if (existeOrdenPendiente) {
                continue; // Evita duplicar órdenes
            }

            Activo activo = activoRepository.findById(plan.getActivoId())
                .orElseThrow(() -> new BusinessException("Activo no encontrado"));

            // 🛠 Crear orden preventiva automática
            OrdenMantenimiento orden = new OrdenMantenimiento();
            orden.setActivo(activo);
            orden.setFechaProgramada(LocalDateTime.now());
            orden.setTipoMantenimiento(plan.getTipoMantenimiento());
            orden.setEstado(EstadoOrden.PENDIENTE);
            orden.setCosto(BigDecimal.ZERO);

            ordenRepository.save(orden);

            // 📅 Actualizar fechas del plan
            plan.setUltimaEjecucion(LocalDateTime.now());
            plan.setProximaEjecucion(LocalDateTime.now().plusDays(plan.getFrecuenciaDias()));

            PlanMantenimiento planAGuardar = new PlanMantenimiento();
            planAGuardar.setTipoMantenimiento(plan.getTipoMantenimiento());
            planAGuardar.setFrecuenciaDias(plan.getFrecuenciaDias());
            planAGuardar.setDescripcion(plan.getDescripcion());
            planAGuardar.setEstaActivo(plan.getEstaActivo());
            planAGuardar.setUltimaEjecucion(plan.getProximaEjecucion());
            planAGuardar.setProximaEjecucion(LocalDateTime.now().plusDays(plan.getFrecuenciaDias()));
            planAGuardar.setActivo(activo);

            repository.save(planAGuardar);
        }
    }

}
