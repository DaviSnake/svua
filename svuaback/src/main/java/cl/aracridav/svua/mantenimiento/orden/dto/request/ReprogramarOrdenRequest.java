package cl.aracridav.svua.mantenimiento.orden.dto.request;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ReprogramarOrdenRequest {
    
    private LocalDateTime nuevaFecha;
}
