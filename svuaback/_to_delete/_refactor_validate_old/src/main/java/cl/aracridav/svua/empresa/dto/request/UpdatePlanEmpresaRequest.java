package cl.aracridav.svua.empresa.dto.request;

import java.time.LocalDate;

import cl.aracridav.svua.empresa.entity.TipoPlan;
import lombok.Data;

@Data
public class UpdatePlanEmpresaRequest {

    private TipoPlan tipoPlan;
    private LocalDate fechaInicioPlan;
    private LocalDate fechaFinPlan;
    private Integer maxUsuarios;
    private Integer maxActivos;
    private Boolean activa;
}
