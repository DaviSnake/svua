package cl.aracridav.svua.mantenimiento.plan.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.aracridav.svua.mantenimiento.plan.entity.PlanMantenimiento;

@Repository
public interface PlanMantenimientoRepository extends JpaRepository<PlanMantenimiento, Long> {
    
    // 🔹 Buscar todos los planes activos
    List<PlanMantenimiento> findByEstaActivoTrue();

    // 🔹 Buscar planes por ID del activo
    List<PlanMantenimiento> findByActivoId(Long activoId);

    // 🔹 Buscar planes activos por activo
    List<PlanMantenimiento> findByActivoIdAndEstaActivoTrue(Long activoId);

    // 🔹 Buscar planes activos cuya próxima ejecución sea hoy o anterior
    List<PlanMantenimiento> 
        findByEstaActivoTrueAndProximaEjecucionLessThanEqual(LocalDate fecha);

     Optional<PlanMantenimiento> findByIdAndEmpresaId(Long id, Long empresaId);

     List<PlanMantenimiento> findByEmpresaId(Long empresaId);

    List<PlanMantenimiento> findByEmpresaIdAndEstaActivoTrue(Long empresaId);

    List<PlanMantenimiento> findByEmpresaIdAndProximaEjecucionLessThanEqual(
            Long empresaId,
            LocalDate fecha
    );
}
