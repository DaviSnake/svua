package cl.aracridav.svua.usuario.dto.response;

import java.time.LocalDateTime;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SesionUsuarioResponse {

    private Long id;
    private Long usuarioId;
    private String usuario;
    private String empresa;

    private LocalDateTime fechaLogin;
    private LocalDateTime ultimaActividad;

    private String paginaActual;
    private String ultimaAccion;

    private String ip;
    private String navegador;
    private String sistemaOperativo;
    private String dispositivo;

    private Integer cantidadRequests;

    private Boolean activa;
}
