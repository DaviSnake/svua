package cl.aracridav.svua.inventario.historial.dto.request;

import lombok.Data;

@Data
public class HistorialEstadoActivoRequest {

    private Long activoId;
    private String comentario;

}
