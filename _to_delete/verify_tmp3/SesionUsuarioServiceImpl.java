package cl.aracridav.svua.usuario.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.usuario.entity.SesionUsuario;
import cl.aracridav.svua.usuario.entity.Usuario;
import cl.aracridav.svua.usuario.repository.SesionUsuarioRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SesionUsuarioServiceImpl implements SesionUsuarioService {

    private final SesionUsuarioRepository sesionRepository;

    @Override
    public SesionUsuario crearSesion(
        Usuario usuario,
        String tokenJti,
        String ip,
        String navegador,
        String sistemaOperativo,
        String dispositivo,
        String versionApp) {

        SesionUsuario sesion =
            SesionUsuario.builder()
                .usuario(usuario)
                .empresa(usuario.getEmpresa())
                .fechaLogin(LocalDateTime.now())
                .ultimaActividad(LocalDateTime.now())
                .activa(true)
                .cantidadRequests(0)
                .tokenJti(tokenJti)
                .paginaActual("Login")
                .ip(ip)
                .navegador(navegador)
                .sistemaOperativo(sistemaOperativo)
                .dispositivo(dispositivo)
                .versionApp(versionApp)
                .build();

        return sesionRepository.save(sesion);
    }

    @Override
    public void actualizarActividad(
        String tokenJti,
        String pagina,
        String accion) {

        sesionRepository.findByTokenJti(tokenJti)
            .ifPresent(sesion -> {

                sesion.setUltimaActividad(
                    LocalDateTime.now());
                sesion.setPaginaActual(pagina);
                sesion.setUltimaAccion(accion);
                sesion.setActiva(true);
                sesion.setCantidadRequests(
                    sesion.getCantidadRequests() + 1);

                sesionRepository.save(sesion);
            });
    }

    @Override
    public void cerrarSesion(String tokenJti) {

        sesionRepository.findByTokenJti(tokenJti)
            .ifPresent(sesion -> {

                sesion.setActiva(false);

                sesion.setFechaLogout(
                    LocalDateTime.now());

                sesionRepository.save(sesion);
            });
    }

    @Override
    public Page<SesionUsuario> obtenerHistorial(
        String usuario,
        Long empresaId,
        LocalDate fecha,
        Pageable pageable) {

        String usuarioFiltro =
            (usuario == null || usuario.isBlank())
                ? null
                : usuario.trim().toLowerCase();

        LocalDateTime desde = fecha != null ? fecha.atStartOfDay() : null;
        LocalDateTime hasta = fecha != null ? fecha.atTime(LocalTime.MAX) : null;

        // 🔥 Se arma con Specification (Criteria API) en vez de JPQL con
        // parámetros nulos: cada predicado solo se agrega si el filtro
        // viene informado, así que nunca se envía un "? IS NULL" sin
        // tipo a Postgres (ver comentario en el repository).
        Specification<SesionUsuario> spec = (root, query, cb) -> {

            Join<SesionUsuario, Usuario> usuarioJoin =
                root.join("usuario", JoinType.INNER);
            Join<SesionUsuario, Empresa> empresaJoin =
                root.join("empresa", JoinType.INNER);

            // El fetch solo aplica a la consulta de datos: en la
            // consulta de conteo (para la paginación) Spring Data pide
            // resultType Long, y ahí un fetch no tiene sentido.
            if (query.getResultType() != Long.class) {
                root.fetch("usuario", JoinType.INNER);
                root.fetch("empresa", JoinType.INNER);
            }

            List<Predicate> predicates = new ArrayList<>();

            if (usuarioFiltro != null) {
                predicates.add(
                    cb.like(
                        cb.lower(usuarioJoin.get("nombre")),
                        "%" + usuarioFiltro + "%"));
            }

            if (empresaId != null) {
                predicates.add(
                    cb.equal(empresaJoin.get("id"), empresaId));
            }

            if (desde != null) {
                predicates.add(
                    cb.greaterThanOrEqualTo(root.get("fechaLogin"), desde));
            }

            if (hasta != null) {
                predicates.add(
                    cb.lessThanOrEqualTo(root.get("fechaLogin"), hasta));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 🔥 Orden fijo por fecha de conexión descendente, sin importar
        // el sort que traiga el Pageable del controller.
        Pageable pageableOrdenado = PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            Sort.by(Sort.Direction.DESC, "fechaLogin"));

        return sesionRepository.findAll(spec, pageableOrdenado);
    }

}
