package cl.aracridav.svua.inventario.ubicacion.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import cl.aracridav.svua.inventario.ubicacion.entity.Ubicacion;

@Repository
public interface UbicacionRepository extends JpaRepository<Ubicacion, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    Page<Ubicacion> findByEmpresaId(Long empresaId, Pageable pegable);

}
