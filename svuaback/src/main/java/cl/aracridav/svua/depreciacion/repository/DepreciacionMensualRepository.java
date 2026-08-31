package cl.aracridav.svua.depreciacion.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import cl.aracridav.svua.depreciacion.dto.DepreciacionDTO;
import cl.aracridav.svua.depreciacion.entity.DepreciacionMensual;
import cl.aracridav.svua.inventario.activo.entity.Activo;

public interface DepreciacionMensualRepository extends JpaRepository<DepreciacionMensual, Long> {
    List<DepreciacionMensual> findByActivoOrderByMesAsc(Activo activo);

    
    @Query("""
        SELECT d.depreciacionMensual
        FROM DepreciacionMensual d
        WHERE d.empresa.id = :empresaId
        ORDER BY d.mes DESC
    """)
    List<BigDecimal> obtenerUltimosMeses(Long empresaId, Pageable pageable);

    @Query("""
        SELECT new cl.aracridav.svua.depreciacion.dto.DepreciacionDTO(
            d.mes,
            SUM(d.depreciacionMensual)
        )
        FROM DepreciacionMensual d
        WHERE d.empresa.id = :empresaId
        GROUP BY d.mes
        ORDER BY d.mes DESC
    """)
    List<DepreciacionDTO> obtenerUltimos(Long empresaId, Pageable pageable);

        // 🔒 La clave es YEAR*100+MONTH (no solo MONTH) para que el
        // llamador pueda calzar cada fila con su mes calendario exacto
        // (año incluido) en vez de solo por posición: si a algun mes del
        // rango no le corresponde ninguna fila (activo creado a mitad de
        // año, por ejemplo), la lista quedaba mas corta y el resto de los
        // meses se corria de lugar contra las etiquetas del grafico.
        @Query("""
        SELECT new cl.aracridav.svua.depreciacion.dto.DepreciacionDTO(
            YEAR(d.fecha) * 100 + MONTH(d.fecha),
            SUM(d.depreciacionMensual)
        )
        FROM DepreciacionMensual d
        WHERE d.empresa.id = :empresaId
        AND d.fecha >= :fechaInicio
        GROUP BY YEAR(d.fecha), MONTH(d.fecha)
        ORDER BY YEAR(d.fecha), MONTH(d.fecha)
    """)
    List<DepreciacionDTO> obtenerUltimos6Meses(Long empresaId, LocalDate fechaInicio, Pageable pageable);

    // 🔒 Depreciación acumulada REAL a la fecha: para cada activo, toma
    // solo su cuota mensual más reciente cuya fecha ya venció (<=
    // :fecha) y suma su depreciacionAcumulada. Antes el dashboard sumaba
    // (valorInicial - valorResidual) desde la tabla `Depreciacion`, que
    // es la base depreciable TOTAL de cada activo durante toda su vida
    // util, no lo depreciado hasta hoy: un activo recien comprado
    // aparecia depreciado al 100% desde el primer dia.
    @Query("""
        SELECT COALESCE(SUM(d.depreciacionAcumulada), 0)
        FROM DepreciacionMensual d
        WHERE d.empresa.id = :empresaId
        AND d.fecha = (
            SELECT MAX(d2.fecha)
            FROM DepreciacionMensual d2
            WHERE d2.activo = d.activo
            AND d2.fecha <= :fecha
        )
    """)
    BigDecimal depreciacionAcumuladaAlDia(Long empresaId, LocalDate fecha);
}
