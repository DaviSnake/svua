package cl.aracridav.svua.usuario.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cl.aracridav.svua.usuario.entity.SesionUsuario;

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

    List<SesionUsuario> findByActivaTrueAndUltimaActividadBefore(LocalDateTime fecha);

    // 🔥 Informe de conexiones (historial completo, paginado y filtrable
    // por usuario / empresa / fecha) usado por el reporte de SUPER_ADMIN.
    //
    // ⚠️ El parámetro :usuario se castea explícitamente a texto (CAST ...
    // AS string) antes de pasarlo por CONCAT/LOWER. Sin el cast, cuando
    // llega null (sin filtro de usuario), PostgreSQL no logra inferir el
    // tipo del bind param dentro de esas funciones y lo trata como bytea,
    // lanzando "function lower(bytea) does not exist" — el (:usuario IS
    // NULL OR ...) no evita esto porque SQL sigue tipando toda la
    // expresión aunque el valor nunca se use.
    @Query(
        value = """
            SELECT s
            FROM SesionUsuario s
            JOIN FETCH s.usuario u
            JOIN FETCH s.empresa e
            WHERE (:usuario IS NULL OR LOWER(u.nombre) LIKE LOWER(CONCAT('%', CAST(:usuario AS string), '%')))
            AND (:empresaId IS NULL OR e.id = :empresaId)
            AND (:desde IS NULL OR s.fechaLogin >= :desde)
            AND (:hasta IS NULL OR s.fechaLogin <= :hasta)
            ORDER BY s.fechaLogin DESC
            """,
        countQuery = """
            SELECT COUNT(s)
            FROM SesionUsuario s
            JOIN s.usuario u
            JOIN s.empresa e
            WHERE (:usuario IS NULL OR LOWER(u.nombre) LIKE LOWER(CONCAT('%', CAST(:usuario AS string), '%')))
            AND (:empresaId IS NULL OR e.id = :empresaId)
            AND (:desde IS NULL OR s.fechaLogin >= :desde)
            AND (:hasta IS NULL OR s.fechaLogin <= :hasta)
            """
    )
    Page<SesionUsuario> buscarHistorial(
        @Param("usuario") String usuario,
        @Param("empresaId") Long empresaId,
        @Param("desde") LocalDateTime desde,
        @Param("hasta") LocalDateTime hasta,
        Pageable pageable
    );

}
