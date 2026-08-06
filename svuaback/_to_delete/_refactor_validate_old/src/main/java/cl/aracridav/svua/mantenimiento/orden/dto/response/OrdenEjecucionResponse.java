package cl.aracridav.svua.mantenimiento.orden.dto.response;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class OrdenEjecucionResponse {

    private Long id;
    private String estado;
    private LocalDateTime fechaEjecucion;
    private LocalDateTime fechaFinEjecucion;
    private Long duracionSegundos;

    private String titulo;

    // opcional (muy útil para UI)
    private String activoNombre;

}
