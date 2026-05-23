package cl.aracridav.svua.mantenimiento.orden.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cl.aracridav.svua.inventario.activo.entity.Activo;
import cl.aracridav.svua.mantenimiento.orden.entity.EstadoOrden;
import cl.aracridav.svua.mantenimiento.orden.entity.OrdenMantenimiento;
import cl.aracridav.svua.mantenimiento.plan.entity.TipoMantenimiento;

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

    @EntityGraph(attributePaths = {
        "repuestosUtilizados",
        "repuestosUtilizados.repuesto"
    })
    List<OrdenMantenimiento> findByEmpresaId(Long empresaId);

    Long countByEmpresaIdAndEstadoIn(Long empresaId, List<EstadoOrden> estados);

    @Query("""
        SELECT COUNT(o)
        FROM OrdenMantenimiento o
        WHERE o.empresa.id = :empresaId
        AND o.fechaProgramada < CURRENT_DATE
        AND o.estado <> 'COMPLETADA'
    """)
    Long countMantenimientosVencidos(Long empresaId);

    @Query("""
        SELECT o.estado, COUNT(o)
        FROM OrdenMantenimiento o
        WHERE o.empresa.id = :empresaId
        GROUP BY o.estado
    """)
    List<Object[]> countOrdenesPorEstado(Long empresaId);

    @Query("""
        SELECT o.activo 
        FROM OrdenMantenimiento o
        WHERE o.id = :ordenId
    """)
    Optional<Activo> findActivoByOrdenId(@Param("ordenId") Long ordenId);

    List<OrdenMantenimiento> findByActivoIdOrderByFechaProgramadaDesc(Long activoId);

    long countByEmpresaId(Long empresaId);

    long countByEmpresaIdAndEstado(Long empresaId, EstadoOrden estado);

    @Query("""
        SELECT COUNT(o)
        FROM OrdenMantenimiento o
        WHERE o.empresa.id = :empresaId
        AND o.estado <> :estado
        AND o.fechaProgramada BETWEEN :inicio AND :fin
        """)
        long contarProgramadas(
            Long empresaId,
            EstadoOrden estado,
            LocalDateTime inicio,
            LocalDateTime fin);

    @Query("""
        SELECT COUNT(o)
        FROM OrdenMantenimiento o
        WHERE o.empresa.id = :empresaId
        AND o.estado = :estado
        AND o.fechaProgramada BETWEEN :inicio AND :fin
        """)
        long contarCompletadas(
            Long empresaId,
            EstadoOrden estado,
            LocalDateTime inicio,
            LocalDateTime fin);

    @Query("""
        SELECT AVG(o.duracionSegundos)
        FROM OrdenMantenimiento o
        WHERE o.empresa.id = :empresaId
        AND o.estado = :estado
        AND o.tipoMantenimiento = :tipo
        AND o.fechaProgramada BETWEEN :inicio AND :fin
        """)
        Double calcularMTTR(
            Long empresaId,
            EstadoOrden estado,
            TipoMantenimiento tipo,
            LocalDateTime inicio,
            LocalDateTime fin);

    @Query("""
        SELECT COUNT(o)
        FROM OrdenMantenimiento o
        WHERE o.empresa.id = :empresaId
        AND o.estado = :estado
        AND o.tipoMantenimiento = :tipo
        AND o.fechaProgramada BETWEEN :inicio AND :fin
        """)
        long contarOrdenesMTTR(
            Long empresaId,
            EstadoOrden estado,
            TipoMantenimiento tipo,
            LocalDateTime inicio,
            LocalDateTime fin);

    @Query("""
        SELECT AVG(o.duracionSegundos)
        FROM OrdenMantenimiento o
        WHERE o.empresa.id = :empresaId
        AND o.estado = 'COMPLETADA'
        AND o.tipoMantenimiento = 'CORRECTIVO'
    """)
    Double avgDuracionByEmpresa(@Param("empresaId") Long empresaId);
    
    @Query("""
        SELECT o
        FROM OrdenMantenimiento o
        WHERE o.empresa.id = :empresaId
        AND o.tipoMantenimiento = 'CORRECTIVO'
        AND o.estado = 'COMPLETADA'
        ORDER BY o.fechaEjecucion ASC
    """)
    List<OrdenMantenimiento> findFallasMTBF(@Param("empresaId") Long empresaId);

}
