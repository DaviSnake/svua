package cl.aracridav.svua.controlturno.dto.response;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PuntoControlResponse {

    private Long id;
    private String nombre;
    private String unidad;
    private BigDecimal valorMin;
    private BigDecimal valorMax;
    private Boolean activo;
}
