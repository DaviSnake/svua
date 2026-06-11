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

        @Query("""
        SELECT new cl.aracridav.svua.depreciacion.dto.DepreciacionDTO(
            MONTH(d.fecha),
            SUM(d.depreciacionMensual)
        )
        FROM DepreciacionMensual d
        WHERE d.empresa.id = :empresaId
        AND d.fecha >= :fechaInicio
        GROUP BY YEAR(d.fecha), MONTH(d.fecha)
        ORDER BY YEAR(d.fecha), MONTH(d.fecha)
    """)
    List<DepreciacionDTO> obtenerUltimos6Meses(Long empresaId, LocalDate fechaInicio, Pageable pageable);
}
