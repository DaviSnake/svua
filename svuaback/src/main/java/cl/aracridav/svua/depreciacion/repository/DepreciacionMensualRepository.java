package cl.aracridav.svua.depreciacion.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.aracridav.svua.depreciacion.entity.DepreciacionMensual;
import cl.aracridav.svua.inventario.activo.entity.Activo;

@Repository
public interface DepreciacionMensualRepository extends JpaRepository<DepreciacionMensual, Long> {
    List<DepreciacionMensual> findByActivoOrderByMesAsc(Activo activo);
}
