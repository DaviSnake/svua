package cl.aracridav.svua.usuario.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import cl.aracridav.svua.usuario.entity.SesionUsuario;

public interface SesionUsuarioRepository
        extends JpaRepository<SesionUsuario, Long>,
                // 🔥 Informe de conexiones: se arma la búsqueda con
                // Specification (Criteria API) en vez de JPQL con
                // parámetros nulos, porque Postgres no logra inferir el
                // tipo de un bind param cuando su ÚNICA aparición es en
                // "? IS NULL" (sin ningún operador/columna tipada al
                // lado) — pasamos por esto dos veces (lower(bytea) con
                // el filtro de usuario, y "could not determine data
                // type of parameter" con las fechas). Con Specification,
                // el predicado simplemente no se agrega si el filtro
                // viene null, así que ese "? IS NULL" nunca llega a la
                // base de datos.
                JpaSpecificationExecutor<SesionUsuario> {

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

}
