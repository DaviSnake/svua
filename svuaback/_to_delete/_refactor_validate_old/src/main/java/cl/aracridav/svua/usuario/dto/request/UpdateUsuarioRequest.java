package cl.aracridav.svua.usuario.dto.request;

import cl.aracridav.svua.shared.enums.RolUsuario;
import lombok.Data;

@Data
public class UpdateUsuarioRequest {

    private String nombre;
    private RolUsuario rol;
    private Boolean activo;
}
