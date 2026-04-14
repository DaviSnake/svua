package cl.aracridav.svua.mantenimiento.orden.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cl.aracridav.svua.mantenimiento.orden.entity.EstadoOrden;
import cl.aracridav.svua.mantenimiento.orden.entity.OrdenMantenimiento;

@Repository
public interface OrdenMantenimientoRepository extends JpaRepository<OrdenMantenimiento, Long> {

    @Query("""
        SELECT o FROM OrdenMantenimiento o
        WHERE o.estado = :estado
    """)
    List<OrdenMantenimiento> findByEstado(
            @Param("estado") String estado
    );

    @Query("""
        SELECT o FROM OrdenMantenimiento o
        WHERE o.activo = :idActivo
    """)
    List<OrdenMantenimiento> findByActivo(
            @Param("idActivo") Long idActivo
    );

    // 🔎 Buscar órdenes por activo
    List<OrdenMantenimiento> findByActivoId(Long activoId);

    // 🔎 Buscar por estado
    List<OrdenMantenimiento> findByEstado(EstadoOrden estado);

    // 🔎 Buscar por activo y estado
    List<OrdenMantenimiento> 
        findByActivoIdAndEstado(Long activoId, EstadoOrden estado);

    // 🔎 Validar si existe orden pendiente para activo
    boolean existsByActivoIdAndEstado(
            Long activoId,
            EstadoOrden estado
    );

    // 🔎 Buscar órdenes entre fechas programadas
    List<OrdenMantenimiento> 
        findByFechaProgramadaBetween(
                LocalDate desde,
                LocalDate hasta
        );

    // 🔎 Buscar por plan de mantenimiento
    List<OrdenMantenimiento> 
        findByPlanMantenimientoId(Long planId);

    // 🔎 Buscar órdenes vencidas
    @Query("""
        SELECT o FROM OrdenMantenimiento o
        WHERE o.estado = 'PENDIENTE'
        AND o.fechaProgramada < :fechaActual
    """)
    List<OrdenMantenimiento> 
        findOrdenesVencidas(@Param("fechaActual") LocalDate fechaActual);

    List<OrdenMantenimiento> findByEmpresaId(Long empresaId);

    Long countByEmpresaIdAndEstadoIn(Long empresaId, List<EstadoOrden> estados);

    @Query("""
        SELECT COUNT(o)
        FROM OrdenMantenimiento o
        WHERE o.empresa.id = :empresaId
        AND o.fechaProgramada < CURRENT_DATE
        AND o.estado <> 'FINALIZADA'
    """)
    Long countMantenimientosVencidos(Long empresaId);

    @Query("""
        SELECT o.estado, COUNT(o)
        FROM OrdenMantenimiento o
        WHERE o.empresa.id = :empresaId
        GROUP BY o.estado
    """)
    List<Object[]> countOrdenesPorEstado(Long empresaId);

}
