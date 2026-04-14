package cl.aracridav.svua.mantenimiento.plan.dto.response;

import java.time.LocalDateTime;

import cl.aracridav.svua.mantenimiento.plan.entity.TipoMantenimiento;
import lombok.Data;

@Data
public class PlanMantenimientoReponse {

    private Long id;
    private TipoMantenimiento tipoMantenimiento;
    private Integer frecuenciaDias;
    private String descripcion;
    private Boolean estaActivo;
    private LocalDateTime ultimaEjecucion;
    private LocalDateTime proximaEjecucion;
    private Long activoId;

}
