package cl.aracridav.svua.mantenimiento.ordenrepuesto.dto.request;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class OrdenRepuestoRequest {

    private Long ordenId;
    private Long repuestoId;
    private Integer cantidad;
    private BigDecimal costoUnitario;

}
