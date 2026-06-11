package cl.aracridav.svua.usuario.service;

import java.time.LocalDateTime;

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

    public void cerrarSesion(String tokenJti) {

        sesionRepository.findByTokenJti(tokenJti)
            .ifPresent(sesion -> {

                sesion.setActiva(false);

                sesion.setFechaLogout(
                    LocalDateTime.now());

                sesionRepository.save(sesion);
            });
    }

}
