package cl.aracridav.svua.mantenimiento.plan.dto.request;

import cl.aracridav.svua.mantenimiento.plan.entity.TipoMantenimiento;
import lombok.Data;

@Data
public class PlanMantenimientoCreateRequest {

    private TipoMantenimiento tipoMantenimiento;
    private Integer frecuenciaDias;
    private String descripcion;
    private Boolean estaActivo;
    private Long activoId;

}
