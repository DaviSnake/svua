package cl.aracridav.svua.proveedor.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import cl.aracridav.svua.proveedor.entity.Proveedor;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    Optional<Proveedor> findByRut(String rut);
    boolean existsByRut(String rut);

    Page<Proveedor> findByEmpresaId(Long empresaId, Pageable pageable);

    boolean existsByRutAndEmpresaId(String rut, Long empresaId);

    // 🔥 Validación de email único (no distingue mayúsculas/minúsculas, como
    // es habitual en direcciones de correo)
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndEmpresaId(String email, Long empresaId);
}
