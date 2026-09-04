package cl.aracridav.svua.controlturno.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cl.aracridav.svua.controlturno.entity.DispositivoEmpresa;

public interface DispositivoEmpresaRepository extends JpaRepository<DispositivoEmpresa, Long> {

    boolean existsByCodigoDispositivoIgnoreCase(String codigoDispositivo);

    // 🔥 Usado por CorreoLecturaImportador para averiguar a que empresa
    // pertenece un dispositivo -- cruza empresas a proposito (por eso
    // necesita bypass_rls, ver esa clase), ya que todavia no se sabe la
    // empresa hasta encontrar este dispositivo.
    Optional<DispositivoEmpresa> findByCodigoDispositivoIgnoreCaseAndActivoTrue(String codigoDispositivo);

    // 🔥 Busqueda unificada para la grilla de administracion (solo
    // SUPER_ADMIN, ve todas las empresas): mismo patron que
    // PuntoControlRepository.buscarPuntosControl.
    @Query("""
        SELECT d FROM DispositivoEmpresa d
        WHERE (:empresaId IS NULL OR d.empresa.id = :empresaId)
        AND (:busqueda IS NULL OR :busqueda = '' OR
             LOWER(d.codigoDispositivo) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR
             LOWER(d.descripcion) LIKE LOWER(CONCAT('%', :busqueda, '%')))
    """)
    Page<DispositivoEmpresa> buscarDispositivos(
        @Param("empresaId") Long empresaId,
        @Param("busqueda") String busqueda,
        Pageable pageable
    );
}
