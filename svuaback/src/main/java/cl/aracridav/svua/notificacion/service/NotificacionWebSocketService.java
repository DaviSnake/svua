package cl.aracridav.svua.notificacion.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import cl.aracridav.svua.notificacion.dto.response.NotificacionSocketResponse;
import cl.aracridav.svua.notificacion.entity.Notificacion;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificacionWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void enviar(Notificacion notificacion) {

        NotificacionSocketResponse dto =
                NotificacionSocketResponse.builder()

                        .id(notificacion.getId())

                        .titulo(notificacion.getTitulo())

                        .mensaje(notificacion.getMensaje())

                        .leida(notificacion.getLeida())

                        .referenciaId(
                                notificacion.getReferenciaId())

                        .tipoReferencia(
                                notificacion.getTipoReferencia())

                        .tipoNotificacion(
                                notificacion.getTipoNotificacion())

                        .fechaCreacion(
                                notificacion.getFechaCreacion())

                        .empresaId(
                                notificacion.getEmpresa().getId())

                        .build();

        messagingTemplate.convertAndSend(

                "/topic/notificaciones/" +
                        dto.getEmpresaId(),

                dto);

    }

}
