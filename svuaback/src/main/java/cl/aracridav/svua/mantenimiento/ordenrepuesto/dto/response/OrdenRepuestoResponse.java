package cl.aracridav.svua.mantenimiento.ordenrepuesto.dto.response;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class OrdenRepuestoResponse {

    private Long id;
    private Long ordenId;
    private Long repuestoId;
    private Integer cantidad;
    private BigDecimal costoUnitario;
    private BigDecimal costoTotal;

}
