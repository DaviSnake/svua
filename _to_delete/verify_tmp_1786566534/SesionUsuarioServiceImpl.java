package cl.aracridav.svua.usuario.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.aracridav.svua.usuario.entity.SesionUsuario;
import cl.aracridav.svua.usuario.entity.Usuario;
import cl.aracridav.svua.usuario.repository.SesionUsuarioRepository;
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
                : usuario.trim();

        LocalDateTime desde = fecha != null ? fecha.atStartOfDay() : null;
        LocalDateTime hasta = fecha != null ? fecha.atTime(LocalTime.MAX) : null;

        return sesionRepository.buscarHistorial(
            usuarioFiltro,
            empresaId,
            desde,
            hasta,
            pageable);
    }

}
