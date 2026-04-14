package cl.aracridav.svua.auth.dto.response;

import cl.aracridav.svua.shared.enums.RolUsuario;
import lombok.*;

@Data
@Builder
public class AuthResponse {
    
    private String accessToken;
    private String refreshToken;
    private String email;
    private RolUsuario rol;
    private Long empresaId;
}
