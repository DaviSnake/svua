package cl.aracridav.svua.inventario.ubicacion.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cl.aracridav.svua.inventario.ubicacion.entity.Ubicacion;

public interface UbicacionRepository extends JpaRepository<Ubicacion, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    Page<Ubicacion> findByEmpresaId(Long empresaId, Pageable pageable);

    Optional<Ubicacion> findFirstByNombre(String nombre);

    boolean existsByNombreIgnoreCaseAndEmpresaId(String nombre, Long empresaId);

    Optional<Ubicacion> findFirstByNombreAndEmpresaId(
        String nombre,
        Long empresaId
    );

    // 🔥 Busqueda unificada para la grilla: empresaId es opcional (NULL =
    // todas las empresas, solo aplica para SUPER_ADMIN) y busqueda es
    // opcional (NULL o vacio = sin filtro de texto). Mismo patron que
    // ActivoRepository.buscarActivos.
    @Query("""
        SELECT u FROM Ubicacion u
        WHERE (:empresaId IS NULL OR u.empresa.id = :empresaId)
        AND (:busqueda IS NULL OR :busqueda = '' OR
             LOWER(u.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')))
    """)
    Page<Ubicacion> buscarUbicaciones(
        @Param("empresaId") Long empresaId,
        @Param("busqueda") String busqueda,
        Pageable pageable
    );

}
