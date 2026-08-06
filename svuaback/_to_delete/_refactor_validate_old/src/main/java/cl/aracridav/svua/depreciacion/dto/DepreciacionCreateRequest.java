package cl.aracridav.svua.depreciacion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import cl.aracridav.svua.depreciacion.entity.MetodoDepreciacion;
import lombok.Data;

@Data
public class DepreciacionCreateRequest {

    private MetodoDepreciacion metodo;
    private BigDecimal valorInicial;
    private BigDecimal valorResidual;
    private Integer vidaUtilMeses;
    private LocalDate fechaInicio;

}
