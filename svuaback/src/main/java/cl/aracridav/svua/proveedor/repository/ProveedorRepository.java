package cl.aracridav.svua.proveedor.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // 🔥 Busqueda unificada para la grilla: empresaId es opcional (NULL =
    // todas las empresas, solo aplica para SUPER_ADMIN) y busqueda es
    // opcional (NULL o vacio = sin filtro de texto). Mismo patron que
    // ActivoRepository.buscarActivos.
    @Query("""
        SELECT p FROM Proveedor p
        WHERE (:empresaId IS NULL OR p.empresa.id = :empresaId)
        AND (:busqueda IS NULL OR :busqueda = '' OR
             LOWER(p.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR
             LOWER(p.rut) LIKE LOWER(CONCAT('%', :busqueda, '%')))
    """)
    Page<Proveedor> buscarProveedores(
        @Param("empresaId") Long empresaId,
        @Param("busqueda") String busqueda,
        Pageable pageable
    );
}
