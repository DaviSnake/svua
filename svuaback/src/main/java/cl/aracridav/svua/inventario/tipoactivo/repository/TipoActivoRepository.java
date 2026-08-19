package cl.aracridav.svua.inventario.tipoactivo.repository;


import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cl.aracridav.svua.inventario.tipoactivo.entity.TipoActivo;

public interface TipoActivoRepository extends JpaRepository<TipoActivo, Long> {

    Page<TipoActivo> findByEmpresaId(Long empresaId, Pageable pageable);

    Optional<TipoActivo> findFirstByNombre(String nombre);

    boolean existsByNombreIgnoreCaseAndEmpresaId(String nombre, Long empresaId);
    
    Optional<TipoActivo> findFirstByNombreAndEmpresaId(
        String nombre,
        Long empresaId
    );

    // 🔥 Busqueda unificada para la grilla: empresaId es opcional (NULL =
    // todas las empresas, solo aplica para SUPER_ADMIN) y busqueda es
    // opcional (NULL o vacio = sin filtro de texto). Mismo patron que
    // ActivoRepository.buscarActivos.
    @Query("""
        SELECT t FROM TipoActivo t
        WHERE (:empresaId IS NULL OR t.empresa.id = :empresaId)
        AND (:busqueda IS NULL OR :busqueda = '' OR
             LOWER(t.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')))
    """)
    Page<TipoActivo> buscarTiposActivo(
        @Param("empresaId") Long empresaId,
        @Param("busqueda") String busqueda,
        Pageable pageable
    );

}
