package cl.aracridav.svua.usuario.service;

import cl.aracridav.svua.usuario.entity.SesionUsuario;
import cl.aracridav.svua.usuario.entity.Usuario;

public interface SesionUsuarioService {

    public SesionUsuario crearSesion(
        Usuario usuario,
        String tokenJti,
        String ip,
        String navegador,
        String sistemaOperativo,
        String dispositivo,
        String versionApp);

    public void actualizarActividad(
        String tokenJti,
        String pagina,
        String accion);

    public void cerrarSesion(String tokenJti);

}
