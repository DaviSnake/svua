package cl.aracridav.svua.auth.dto.request;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;

    private String navegador;
    private String sistemaOperativo;
    private String dispositivo;
    private String versionApp;
}
