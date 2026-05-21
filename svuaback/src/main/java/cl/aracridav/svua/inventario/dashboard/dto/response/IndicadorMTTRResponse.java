package cl.aracridav.svua.inventario.dashboard.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
public class IndicadorMTTRResponse {

    private double mttrHoras;

    private long ordenesConsideradas;
}
