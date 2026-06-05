package cl.aracridav.svua.inventario.historial.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HistorialActivoResponse {

    private LocalDateTime fecha;
    private String tipo;
    private String descripcion;
    private String usuario;
    private BigDecimal costoTotal;
    private BigDecimal valorHora;
    private BigDecimal costoManoObra;
    private BigDecimal horasTrabajo;
}
