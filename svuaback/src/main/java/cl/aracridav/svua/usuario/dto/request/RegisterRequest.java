package cl.aracridav.svua.usuario.dto.request;

import cl.aracridav.svua.shared.enums.RolUsuario;
import lombok.Data;

@Data
public class RegisterRequest {

    private String nombre;
    private String email;
    private String password;
    private RolUsuario rol;
    private Long empresaId;

}
