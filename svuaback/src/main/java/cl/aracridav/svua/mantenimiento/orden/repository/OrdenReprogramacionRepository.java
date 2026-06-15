package cl.aracridav.svua.mantenimiento.orden.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import cl.aracridav.svua.mantenimiento.orden.entity.OrdenReprogramacion;

public interface OrdenReprogramacionRepository extends JpaRepository<OrdenReprogramacion, Long> {

    @Modifying
    @Transactional
    @Query("""
        DELETE FROM OrdenReprogramacion o
        WHERE o.empresa.id = :empresaId
    """)
    int deleteByEmpresaId(@Param("empresaId") Long empresaId);

}
