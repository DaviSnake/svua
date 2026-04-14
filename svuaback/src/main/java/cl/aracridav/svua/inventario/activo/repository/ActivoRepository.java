package cl.aracridav.svua.inventario.activo.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cl.aracridav.svua.inventario.activo.entity.Activo;
import cl.aracridav.svua.shared.enums.EstadoActivo;

@Repository
public interface ActivoRepository extends JpaRepository<Activo, Long> {

    boolean existsByCodigoInterno(String codigoInterno);

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
}
