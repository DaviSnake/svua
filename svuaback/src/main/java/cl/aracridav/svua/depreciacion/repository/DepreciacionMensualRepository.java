package cl.aracridav.svua.depreciacion.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import cl.aracridav.svua.depreciacion.dto.DepreciacionDTO;
import cl.aracridav.svua.depreciacion.entity.DepreciacionMensual;
import cl.aracridav.svua.depreciacion.entity.TipoDepreciacion;
import cl.aracridav.svua.inventario.activo.entity.Activo;

public interface DepreciacionMensualRepository extends JpaRepository<DepreciacionMensual, Long> {

    // 🔒 Filtrado por tipo: desde que existe el cronograma ACELERADA
    // (ver TipoDepreciacion), cada activo tiene DOS cuotas por mes en
    // esta misma tabla. Sin filtrar por tipo, cualquier consulta que
    // liste o sume `DepreciacionMensual` mezclaria ambos cronogramas.
    List<DepreciacionMensual> findByActivoAndTipoOrderByMesAsc(Activo activo, TipoDepreciacion tipo);

    // 🔎 Idempotencia del backfill de depreciación acelerada: evita
    // recalcular (y duplicar) el cronograma de un activo que ya lo tiene.
    boolean existsByActivoIdAndTipo(Long activoId, TipoDepreciacion tipo);

    // 🔒 Todas las agregaciones para reportes financieros/dashboard se
    // acotan a tipo = NORMAL: la depreciacion ACELERADA es un calculo
    // puramente tributario (para el impuesto a la renta), no debe
    // sumarse junto a la contable ni mostrarse en esos KPIs.
    @Query("""
        SELECT new cl.aracridav.svua.depreciacion.dto.DepreciacionDTO(
            YEAR(d.fecha) * 100 + MONTH(d.fecha),
            SUM(d.depreciacionMensual)
        )
        FROM DepreciacionMensual d
        WHERE d.empresa.id = :empresaId
        AND d.tipo = cl.aracridav.svua.depreciacion.entity.TipoDepreciacion.NORMAL
        AND d.fecha >= :fechaInicio
        GROUP BY YEAR(d.fecha), MONTH(d.fecha)
        ORDER BY YEAR(d.fecha), MONTH(d.fecha)
    """)
    List<DepreciacionDTO> obtenerUltimos6Meses(Long empresaId, LocalDate fechaInicio, Pageable pageable);

    // Depreciación acumulada REAL a la fecha: para cada activo, toma
    // solo su cuota mensual más reciente cuya fecha ya venció (<=
    // :fecha) y suma su depreciacionAcumulada.
    @Query("""
        SELECT COALESCE(SUM(d.depreciacionAcumulada), 0)
        FROM DepreciacionMensual d
        WHERE d.empresa.id = :empresaId
        AND d.tipo = cl.aracridav.svua.depreciacion.entity.TipoDepreciacion.NORMAL
        AND d.fecha = (
            SELECT MAX(d2.fecha)
            FROM DepreciacionMensual d2
            WHERE d2.activo = d.activo
            AND d2.tipo = cl.aracridav.svua.depreciacion.entity.TipoDepreciacion.NORMAL
            AND d2.fecha <= :fecha
        )
    """)
    BigDecimal depreciacionAcumuladaAlDia(Long empresaId, LocalDate fecha);
}
