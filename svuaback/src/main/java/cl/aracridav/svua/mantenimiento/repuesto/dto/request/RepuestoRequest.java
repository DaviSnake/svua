package cl.aracridav.svua.mantenimiento.repuesto.dto.request;

import java.math.BigDecimal;

import cl.aracridav.svua.mantenimiento.repuesto.entity.TipoRepuesto;
import lombok.Data;

@Data
public class RepuestoRequest {

    private String codigo;
    private String nombre;
    private String descripcion;
    private BigDecimal costoUnitario;
    private Integer stockMinimo;
    private Integer stockActual;
    private TipoRepuesto tipoRepuesto;
    private Long empresaId;
    private Boolean activo;

}
