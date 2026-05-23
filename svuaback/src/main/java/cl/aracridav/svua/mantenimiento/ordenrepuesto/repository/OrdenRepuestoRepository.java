package cl.aracridav.svua.mantenimiento.ordenrepuesto.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cl.aracridav.svua.mantenimiento.ordenrepuesto.entity.OrdenRepuesto;

@Repository
public interface OrdenRepuestoRepository extends JpaRepository<OrdenRepuesto, Long> {

    List<OrdenRepuesto> findByOrdenId(Long ordenId);

    @Query("""
        SELECT COALESCE(SUM(o.costoTotal), 0)
        FROM OrdenRepuesto o
        WHERE o.orden.id = :ordenId
    """)
    BigDecimal calcularCostoOrden(Long ordenId);

}
