package cl.aracridav.svua.inventario.activo.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cl.aracridav.svua.inventario.activo.entity.Activo;
import cl.aracridav.svua.shared.enums.EstadoActivo;

public interface ActivoRepository extends JpaRepository<Activo, Long> {

    boolean existsByCodigoInterno(String codigoInterno);

    // 🔳 Usados para el escaneo de QR/EAN13 (buscar el activo por cualquiera
    // de los dos codigos, filtrando por empresa salvo para SUPER_ADMIN).
    Optional<Activo> findByCodigoInterno(String codigoInterno);
    Optional<Activo> findByCodigoEan13(String codigoEan13);
    Optional<Activo> findByCodigoInternoAndEmpresaId(String codigoInterno, Long empresaId);
    Optional<Activo> findByCodigoEan13AndEmpresaId(String codigoEan13, Long empresaId);

    @Query("""
        SELECT a FROM Activo a
        WHERE a.estadoActual = :estado
    """)
    List<Activo> findByEstado(@Param("estado") String estado);

    @Query("""
        SELECT a FROM Activo a
        WHERE a.fechaBaja IS NULL
    """)
    List<Activo> findActivosOperativos();

    @Query("""
        SELECT a FROM Activo a
        WHERE a.ubicacion = :idUbicacion
    """)
    List<Activo> findByUbicacion(@Param("idUbicacion") Long idUbicacion);

    Long countByEmpresaId(Long empresaId);

    Long countByEmpresaIdAndEstadoActual(Long empresaId, EstadoActivo estado);

    @Query("""
        SELECT COALESCE(SUM(a.valorAdquisicion),0)
        FROM Activo a
        WHERE a.empresa.id = :empresaId
    """)
    BigDecimal sumValorByEmpresa(@Param("empresaId") Long empresaId);

    Optional<Activo> findFirstByNombre(String nombre);

    Optional<Activo> findByNombreContainingIgnoreCase(String nombre);

    @Query("""
        SELECT DISTINCT a
        FROM Activo a
        LEFT JOIN FETCH a.historialEstados he
        LEFT JOIN FETCH he.usuario
        LEFT JOIN FETCH a.ordenesMantenimiento om
        LEFT JOIN FETCH om.usuario
        LEFT JOIN FETCH om.proveedor
        LEFT JOIN FETCH om.repuestosUtilizados ru
        LEFT JOIN FETCH ru.repuesto
        """)
    List<Activo> findAllConHistorial();

    @Query("""
        SELECT DISTINCT a
        FROM Activo a
        LEFT JOIN FETCH a.historialEstados he
        LEFT JOIN FETCH he.usuario
        LEFT JOIN FETCH a.ordenesMantenimiento om
        LEFT JOIN FETCH om.usuario
        LEFT JOIN FETCH om.proveedor
        WHERE a.empresa.id = :empresaId
    """)
    List<Activo> findAllConHistorialByEmpresa(
        @Param("empresaId") Long empresaId
    );

    Optional<Activo> findFirstByNombreAndEmpresaId(
        String nombre,
        Long empresaId
    );

    List<Activo> findByEmpresaId(Long empresaId);

    // 🔥 Paginado, usado por el listado de la grilla (mostrarActivos) para
    // restringir por empresa cuando corresponde.
    Page<Activo> findByEmpresaId(Long empresaId, Pageable pageable);

    // 🔍 Busqueda por codigo interno o nombre (mantenedor de Activo), con
    // filtro opcional por empresa (empresaId null = todas, usado por
    // SUPER_ADMIN sin filtro seleccionado).
    @Query("""
        SELECT a FROM Activo a
        WHERE (:empresaId IS NULL OR a.empresa.id = :empresaId)
        AND (:busqueda IS NULL OR :busqueda = '' OR
             LOWER(a.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR
             LOWER(a.codigoInterno) LIKE LOWER(CONCAT('%', :busqueda, '%')))
    """)
    Page<Activo> buscarActivos(
        @Param("empresaId") Long empresaId,
        @Param("busqueda") String busqueda,
        Pageable pageable
    );

    // 🔎 Activos de la empresa que aún no tienen su cronograma de
    // depreciación ACELERADA calculado: creados antes de que existiera
    // ese cronograma, o cargados por importación masiva (que solo
    // genera la depreciación NORMAL). Base para el backfill de
    // DepreciacionServiceImpl.generarDepreciacionAceleradaFaltante().
    @Query("""
        SELECT a
        FROM Activo a
        WHERE a.empresa.id = :empresaId
        AND NOT EXISTS (
            SELECT 1
            FROM DepreciacionMensual d
            WHERE d.activo = a
            AND d.tipo = cl.aracridav.svua.depreciacion.entity.TipoDepreciacion.ACELERADA
        )
    """)
    List<Activo> findActivosSinDepreciacionAcelerada(@Param("empresaId") Long empresaId);
}
