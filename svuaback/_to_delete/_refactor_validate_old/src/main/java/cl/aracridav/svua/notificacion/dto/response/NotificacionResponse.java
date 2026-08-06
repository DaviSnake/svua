package cl.aracridav.svua.notificacion.dto.response;

import java.time.LocalDateTime;

import lombok.*;

@Data
@Builder
public class NotificacionResponse {

    private Long id;
    private String titulo;
    private String mensaje;
    private Boolean leida;

    private Long referenciaId;
    private String tipoReferencia;

    private String tipoNotificacion;

    private LocalDateTime fechaCreacion;

}