package cl.aracridav.svua.mantenimiento.ordenrepuesto.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cl.aracridav.svua.mantenimiento.ordenrepuesto.entity.OrdenRepuesto;

public interface OrdenRepuestoRepository extends JpaRepository<OrdenRepuesto, Long> {

    List<OrdenRepuesto> findByOrdenId(Long ordenId);

    @Query("""
        SELECT COALESCE(SUM(o.costoTotal), 0)
        FROM OrdenRepuesto o
        WHERE o.orden.id = :ordenId
    """)
    BigDecimal calcularCostoOrden(Long ordenId);

    Optional<OrdenRepuesto> findByOrdenIdAndRepuestoId(
            Long ordenId,
            Long repuestoId
    );

    @Modifying
    @Query("""
        DELETE FROM OrdenRepuesto r
        WHERE r.orden.empresa.id = :empresaId
    """)
    void deleteByEmpresaId(@Param("empresaId") Long empresaId);

}
