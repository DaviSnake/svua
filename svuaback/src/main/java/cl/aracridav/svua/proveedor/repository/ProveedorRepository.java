package cl.aracridav.svua.proveedor.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.aracridav.svua.proveedor.entity.Proveedor;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    Optional<Proveedor> findByRut(String rut);
    boolean existsByRut(String rut);

    Page<Proveedor> findByEmpresaId(Long empresaId, Pageable pegable);
}
