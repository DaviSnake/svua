package cl.aracridav.svua.controlturno.dto.request;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class PuntoControlRequest {

    private String nombre;
    private String unidad;
    private BigDecimal valorMin;
    private BigDecimal valorMax;
    private Long empresaId;
}
