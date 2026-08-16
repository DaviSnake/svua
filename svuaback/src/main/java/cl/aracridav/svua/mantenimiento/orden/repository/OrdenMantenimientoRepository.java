package cl.aracridav.svua.mantenimiento.orden.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cl.aracridav.svua.inventario.activo.entity.Activo;
import cl.aracridav.svua.mantenimiento.orden.entity.EstadoOrden;
import cl.aracridav.svua.mantenimiento.orden.entity.OrdenMantenimiento;
import cl.aracridav.svua.mantenimiento.orden.entity.TipoMantenimiento;

public interface OrdenMantenimientoRepository extends JpaRepository<OrdenMantenimiento, Long>,
        // 🔥 Informe de Mantenciones: se arma el filtro con Specification
        // (Criteria API), igual que en SesionUsuarioRepository, para no
        // enviar nunca un bind param sin tipo ("? IS NULL") a Postgres.
        JpaSpecificationExecutor<OrdenMantenimiento> {

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

    // Solo cuentan como vencidas las ordenes en estado PROGRAMADA cuya
    // fecha programada ya paso. Una orden EN_EJECUCION (o en cualquier
    // otro estado distinto de PROGRAMADA) no debe sumar aqui, aunque su
    // fecha programada original haya quedado atras: ya esta en curso, no
    // esta "atrasada sin iniciar".
    @Query("""
        SELECT COUNT(o)
        FROM OrdenMantenimiento o
        WHERE o.empresa.id = :empresaId
        AND o.fechaProgramada < CURRENT_DATE
        AND o.estado = 'PROGRAMADA'
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

    long countByEmpresaIdAndEstadoNot(Long empresaId, EstadoOrden estado);

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

    @Query("""
        SELECT 
            YEAR(o.fechaProgramada),
            MONTH(o.fechaProgramada),
            COALESCE(SUM(o.costoTotal), 0)
        FROM OrdenMantenimiento o
        WHERE o.fechaProgramada >= :fechaInicio
        GROUP BY YEAR(o.fechaProgramada), MONTH(o.fechaProgramada)
        ORDER BY YEAR(o.fechaProgramada), MONTH(o.fechaProgramada)
    """)
    List<Object[]> obtenerCostosUltimosMeses(
        @Param("fechaInicio") LocalDateTime fechaInicio);

    @Query("""
        SELECT 
            YEAR(o.fechaEjecucion),
            MONTH(o.fechaEjecucion),
            COALESCE(SUM(o.costoTotal), 0)
        FROM OrdenMantenimiento o
        WHERE o.fechaEjecucion >= :fechaInicio
        AND o.activo.empresa.id = :empresaId
        AND o.estado = 'COMPLETADA'
        GROUP BY YEAR(o.fechaEjecucion), MONTH(o.fechaEjecucion)
        ORDER BY YEAR(o.fechaEjecucion), MONTH(o.fechaEjecucion)
    """)
    List<Object[]> obtenerCostosUltimosMeses(
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("empresaId") Long empresaId);

    // 🔥 Mismo grafico de costos, pero acotado a un activo puntual
    // (filtro opcional en el frontend: "Evolucion Costo de Mantencion").
    // Se hace un metodo separado en vez de un ":activoId IS NULL OR ..."
    // en la misma query, siguiendo el mismo patron ya usado arriba para
    // empresaId: en Postgres un bind param que solo aparece dentro de
    // "? IS NULL" no siempre puede inferir su tipo.
    @Query("""
        SELECT 
            YEAR(o.fechaEjecucion),
            MONTH(o.fechaEjecucion),
            COALESCE(SUM(o.costoTotal), 0)
        FROM OrdenMantenimiento o
        WHERE o.fechaEjecucion >= :fechaInicio
        AND o.activo.empresa.id = :empresaId
        AND o.activo.id = :activoId
        AND o.estado = 'COMPLETADA'
        GROUP BY YEAR(o.fechaEjecucion), MONTH(o.fechaEjecucion)
        ORDER BY YEAR(o.fechaEjecucion), MONTH(o.fechaEjecucion)
    """)
    List<Object[]> obtenerCostosUltimosMesesPorActivo(
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("empresaId") Long empresaId,
        @Param("activoId") Long activoId);

    List<OrdenMantenimiento> findByEstadoAndTipoMantenimientoAndFechaProgramadaBetween(
        EstadoOrden estado,
        TipoMantenimiento tipoMantenimiento,
        LocalDateTime desde,
        LocalDateTime hasta
    );

    @Modifying
    @Query("""
        DELETE FROM OrdenMantenimiento o
        WHERE o.empresa.id = :empresaId
    """)
    void deleteByEmpresaId(@Param("empresaId") Long empresaId);

}
