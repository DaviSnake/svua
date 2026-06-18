package cl.aracridav.svua.inventario.ubicacion.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import cl.aracridav.svua.inventario.ubicacion.entity.Ubicacion;

public interface UbicacionRepository extends JpaRepository<Ubicacion, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    Page<Ubicacion> findByEmpresaId(Long empresaId, Pageable pegable);

    Optional<Ubicacion> findFirstByNombre(String nombre);

    boolean existsByNombreIgnoreCaseAndEmpresaId(String nombre, Long empresaId);

    Optional<Ubicacion> findFirstByNombreAndEmpresaId(
        String nombre,
        Long empresaId
    );

}
