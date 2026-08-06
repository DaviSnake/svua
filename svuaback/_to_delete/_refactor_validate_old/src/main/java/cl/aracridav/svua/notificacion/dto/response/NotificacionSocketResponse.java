package cl.aracridav.svua.notificacion.dto.response;

import java.time.LocalDateTime;

import cl.aracridav.svua.notificacion.entity.TipoNotificacion;
import cl.aracridav.svua.notificacion.entity.TipoReferencia;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionSocketResponse {

    private Long id;

    private String titulo;

    private String mensaje;

    private Boolean leida;

    private Long referenciaId;

    private TipoReferencia tipoReferencia;

    private TipoNotificacion tipoNotificacion;

    private LocalDateTime fechaCreacion;

    private Long empresaId;

}
