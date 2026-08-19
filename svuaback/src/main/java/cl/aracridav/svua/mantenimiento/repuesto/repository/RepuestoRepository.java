package cl.aracridav.svua.mantenimiento.repuesto.repository;


import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.mantenimiento.repuesto.entity.Repuesto;

public interface RepuestoRepository extends JpaRepository<Repuesto, Long> {

    Page<Repuesto> findByEmpresa(Empresa empresa, Pageable pegable);

    @EntityGraph(attributePaths = {"empresa"})
    Page<Repuesto> findByEmpresaId(Long empresaId, Pageable pageable);

    boolean existsByCodigoAndEmpresa(String codigo, Empresa empresa);

    List<Repuesto> findByEmpresaId(Long empresaId);

    // 🔥 Busqueda unificada para la grilla: empresaId es opcional (NULL =
    // todas las empresas, solo aplica para SUPER_ADMIN) y busqueda es
    // opcional (NULL o vacio = sin filtro de texto). Mismo patron que
    // ActivoRepository.buscarActivos.
    @EntityGraph(attributePaths = {"empresa"})
    @Query("""
        SELECT r FROM Repuesto r
        WHERE (:empresaId IS NULL OR r.empresa.id = :empresaId)
        AND (:busqueda IS NULL OR :busqueda = '' OR
             LOWER(r.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR
             LOWER(r.codigo) LIKE LOWER(CONCAT('%', :busqueda, '%')))
    """)
    Page<Repuesto> buscarRepuestos(
        @Param("empresaId") Long empresaId,
        @Param("busqueda") String busqueda,
        Pageable pageable
    );

}
