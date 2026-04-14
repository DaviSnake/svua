package cl.aracridav.svua.depreciacion.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cl.aracridav.svua.depreciacion.entity.Depreciacion;
import cl.aracridav.svua.depreciacion.entity.MetodoDepreciacion;

@Repository
public interface DepreciacionRepository extends JpaRepository<Depreciacion, Long> {

    // 🔎 Buscar depreciación por activo
    Optional<Depreciacion> findByActivoId(Long activoId);

    // 🔎 Verificar si un activo ya tiene depreciación configurada
    boolean existsByActivoId(Long activoId);

    // 🔎 Listar depreciaciones por método
    List<Depreciacion> findByMetodo(MetodoDepreciacion metodo);

    @Query("""
       SELECT d 
       FROM Depreciacion d
       WHERE d.fechaInicio <= :fecha
       """)
    List<Depreciacion> obtenerDepreciacionesVigentes(
            @Param("fecha") LocalDate fecha);

    @Query("""
       SELECT d
       FROM Depreciacion d
       JOIN d.activo a
       WHERE a.ubicacion.id = :ubicacionId
       """)
    List<Depreciacion> findByUbicacion(@Param("ubicacionId") Long ubicacionId);

   @Query("""
        SELECT COALESCE(SUM(d.valorInicial - d.valorResidual),0)
        FROM Depreciacion d
        WHERE d.empresa.id = :empresaId
    """)
    BigDecimal depreciacionTotal(@Param("empresaId") Long empresaId);

}
