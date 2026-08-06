package cl.aracridav.svua.inventario.dashboard.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndicadorCumplimientoResponse {

    private long programadas;

    private long completadas;

    private double cumplimiento;
}
