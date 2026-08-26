package cl.aracridav.svua.controlturno.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import cl.aracridav.svua.controlturno.enums.TurnoTrabajo;
import lombok.Data;

@Data
public class LecturaControlRequest {

    private Long puntoControlId;
    private BigDecimal valor;

    // 🔥 Opcional: si no se informa, el servicio usa la hora actual del
    // servidor (ver LecturaControlServiceImpl.registrar).
    private LocalDateTime fechaHora;

    private TurnoTrabajo turno;
    private String observacion;
}
