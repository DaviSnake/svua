package cl.aracridav.svua.mantenimiento.repuesto.dto.request;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class RepuestoRequest {

    private String codigo;
    private String nombre;
    private String descripcion;
    private BigDecimal costoUnitario;
    private Integer stockMinimo;
    private Boolean activo;

}
