package cl.aracridav.svua.inventario.historial.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cl.aracridav.svua.inventario.activo.entity.Activo;
import cl.aracridav.svua.inventario.historial.entity.HistorialEstadoActivo;
import cl.aracridav.svua.shared.enums.EstadoActivo;

@Repository
public interface HistorialEstadoActivoRepository extends JpaRepository<HistorialEstadoActivo, Long> {

    @Query("""
        SELECT h FROM HistorialEstadoActivo h
        WHERE h.activo = :idActivo
        ORDER BY h.fecha DESC
    """)
    List<HistorialEstadoActivo> findHistorialPorActivo(
            @Param("idActivo") Long idActivo
    );

    // 🔎 Obtener historial completo de un activo (ordenado por fecha)
    List<HistorialEstadoActivo> 
        findByActivoIdOrderByFechaAsc(Long activoId);

    // 🔎 Obtener historial descendente (último primero)
    List<HistorialEstadoActivo> 
        findByActivoIdOrderByFechaDesc(Long activoId);

    // 🔎 Obtener último estado del activo
    Optional<HistorialEstadoActivo> 
        findTopByActivoIdOrderByFechaDesc(Long activoId);

    // 🔎 Buscar por estado específico
    List<HistorialEstadoActivo> 
        findByActivoIdAndEstado(Long activoId, EstadoActivo estado);

    // 🔎 Buscar por rango de fechas
    List<HistorialEstadoActivo> 
        findByActivoIdAndFechaBetween(
                Long activoId,
                LocalDateTime inicio,
                LocalDateTime fin
        );

    @Query("""
       SELECT h
       FROM HistorialEstadoActivo h
       WHERE h.fecha BETWEEN :inicio AND :fin
       """)
    List<HistorialEstadoActivo> 
    findByRangoFechas(@Param("inicio") LocalDateTime inicio,
                      @Param("fin") LocalDateTime fin);

    @Query("""
       SELECT h
       FROM HistorialEstadoActivo h
       WHERE h.activo.id = :activoId
       AND h.estado = :estado
       ORDER BY h.fecha ASC
       """)
    List<HistorialEstadoActivo> 
    findEventosPorEstado(@Param("activoId") Long activoId,
                         @Param("estado") EstadoActivo estado);

    List<HistorialEstadoActivo> findByActivoOrderByFechaDesc(Activo activo);

}
