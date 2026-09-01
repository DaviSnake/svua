package cl.aracridav.svua.controlturno.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cl.aracridav.svua.controlturno.entity.PuntoControl;

public interface PuntoControlRepository extends JpaRepository<PuntoControl, Long> {

    boolean existsByNombreIgnoreCaseAndEmpresaId(String nombre, Long empresaId);

    // 🔥 Usado por HojaControlImportServiceImpl para el patron
    // "buscar o crear" al importar el Excel: un punto de control se crea
    // solo la primera vez que aparece en un archivo importado.
    Optional<PuntoControl> findByNombreIgnoreCaseAndEmpresaId(String nombre, Long empresaId);

    List<PuntoControl> findByEmpresaIdAndActivoTrue(Long empresaId);

    // 🔥 Busqueda unificada para la grilla de administracion: empresaId
    // es opcional (NULL = todas las empresas, solo aplica para
    // SUPER_ADMIN) y busqueda es opcional (NULL o vacio = sin filtro de
    // texto). Mismo patron que UbicacionRepository.buscarUbicaciones.
    @Query("""
        SELECT p FROM PuntoControl p
        WHERE (:empresaId IS NULL OR p.empresa.id = :empresaId)
        AND (:busqueda IS NULL OR :busqueda = '' OR
             LOWER(p.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')))
    """)
    Page<PuntoControl> buscarPuntosControl(
        @Param("empresaId") Long empresaId,
        @Param("busqueda") String busqueda,
        Pageable pageable
    );
}
