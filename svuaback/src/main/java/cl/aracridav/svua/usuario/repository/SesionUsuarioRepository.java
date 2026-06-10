package cl.aracridav.svua.usuario.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cl.aracridav.svua.usuario.entity.SesionUsuario;

@Repository
public interface SesionUsuarioRepository
        extends JpaRepository<SesionUsuario, Long> {

    List<SesionUsuario> findByActivaTrue();

    Optional<SesionUsuario> findByTokenJti(String tokenJti);

    List<SesionUsuario> findByUsuarioIdAndActivaTrue(Long usuarioId);

    @Query("""
        SELECT s
        FROM SesionUsuario s
        WHERE s.activa = true
        AND s.ultimaActividad >= :limite
        """)
        List<SesionUsuario> usuariosActivos(
            LocalDateTime limite
    );

    @Query("""
        SELECT s
        FROM SesionUsuario s
        JOIN FETCH s.usuario
        JOIN FETCH s.empresa
        WHERE s.activa = true
    """)
    List<SesionUsuario> findActivasConUsuarioEmpresa();

}
