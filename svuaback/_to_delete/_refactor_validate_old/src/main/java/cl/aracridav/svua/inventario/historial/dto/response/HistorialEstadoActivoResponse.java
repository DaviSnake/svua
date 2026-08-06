package cl.aracridav.svua.inventario.historial.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import cl.aracridav.svua.shared.enums.EstadoActivo;
import lombok.Data;

@Data
@JsonPropertyOrder({
    "id",
    "estado",
    "fecha",
    "comentario"
})
public class HistorialEstadoActivoResponse {

    private Long id;
    private EstadoActivo estado;
    private LocalDateTime fecha;
    private String comentario;

}
