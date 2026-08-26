package cl.aracridav.svua.controlturno.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import cl.aracridav.svua.controlturno.enums.TurnoTrabajo;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LecturaControlResponse {

    private Long id;
    private Long puntoControlId;
    private String puntoControlNombre;
    private String unidad;
    private BigDecimal valor;
    private LocalDateTime fechaHora;
    private TurnoTrabajo turno;
    private String observacion;
    private String usuarioNombre;
}
