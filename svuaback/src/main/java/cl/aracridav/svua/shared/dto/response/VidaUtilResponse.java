package cl.aracridav.svua.shared.dto.response;

import java.time.LocalDate;

import cl.aracridav.svua.shared.enums.EstadoVidaUtil;
import lombok.*;

@Data
@Builder
public class VidaUtilResponse {

    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private int mesesTotales;
    private int mesesConsumidos;
    private int mesesRestantes;
    private double porcentajeConsumido;
    private EstadoVidaUtil estado;

}
