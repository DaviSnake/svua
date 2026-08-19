package cl.aracridav.svua.inventario.bodega.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.inventario.bodega.entity.Bodega;

public interface BodegaRepository extends JpaRepository<Bodega, Long> {

    Page<Bodega> findByEmpresaId(Long empresaId, Pageable pageable);

    List<Bodega> findByEmpresaAndActivaTrue(Empresa empresa);

    boolean existsByNombreIgnoreCaseAndEmpresaId(String nombre, Long empresaId);

    // 🔥 Busqueda unificada para la grilla: empresaId es opcional (NULL =
    // todas las empresas, solo aplica para SUPER_ADMIN) y busqueda es
    // opcional (NULL o vacio = sin filtro de texto). Mismo patron que
    // ActivoRepository.buscarActivos.
    @Query("""
        SELECT b FROM Bodega b
        WHERE (:empresaId IS NULL OR b.empresa.id = :empresaId)
        AND (:busqueda IS NULL OR :busqueda = '' OR
             LOWER(b.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')))
    """)
    Page<Bodega> buscarBodegas(
        @Param("empresaId") Long empresaId,
        @Param("busqueda") String busqueda,
        Pageable pageable
    );

}
