package cl.aracridav.svua.notificacion.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.notificacion.entity.Notificacion;
import cl.aracridav.svua.notificacion.entity.TipoNotificacion;
import cl.aracridav.svua.notificacion.entity.TipoReferencia;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    Optional<Notificacion> findByReferenciaIdAndTipoReferenciaAndTipoNotificacion(
            Long referenciaId,
            TipoReferencia tipoReferencia,
            TipoNotificacion tipoNotificacion
    );

    long countByLeidaFalse();

    Optional<Notificacion> findById(Long id);

    long countByEmpresaAndLeidaFalse(
        Empresa empresa
    );

    List<Notificacion> findByEmpresaOrderByFechaCreacionDesc(
        Empresa empresa);

}
