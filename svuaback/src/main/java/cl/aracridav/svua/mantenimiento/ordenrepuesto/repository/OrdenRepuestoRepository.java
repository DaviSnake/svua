package cl.aracridav.svua.mantenimiento.ordenrepuesto.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.aracridav.svua.mantenimiento.ordenrepuesto.entity.OrdenRepuesto;

@Repository
public interface OrdenRepuestoRepository extends JpaRepository<OrdenRepuesto, Long> {

    List<OrdenRepuesto> findByOrdenId(Long ordenId);

}
