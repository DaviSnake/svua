package cl.aracridav.svua.usuario.dto.response;

import cl.aracridav.svua.shared.enums.RolUsuario;
import lombok.*;

@Data
@Builder
public class UsuarioResponse {
    
    private Long id;
    private String nombre;
    private RolUsuario rol;
    private String email;
    private Boolean activo;

    private Long empresaId;
    private String empresaNombre;
}
