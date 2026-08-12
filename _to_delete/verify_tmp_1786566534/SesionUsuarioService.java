package cl.aracridav.svua.usuario.service;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    // 🔥 Informe de conexiones: historial paginado y filtrable por usuario,
    // empresa y fecha, ordenado por fecha de conexión descendente.
    public Page<SesionUsuario> obtenerHistorial(
        String usuario,
        Long empresaId,
        LocalDate fecha,
        Pageable pageable);

}
