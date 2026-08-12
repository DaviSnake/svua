package cl.aracridav.svua.inventario.historial.dto.response;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HistorialActivoCompletoResponse {

    private Long activoId;
    private String nombreActivo;
    private BigDecimal valorAdquisicion;
    private BigDecimal valorResidual;
    private BigDecimal costoMantenciones;
    private Integer cantidadMantenciones;
    private Long empresaId;
    private String empresaNombre;
    private List<HistorialActivoResponse> eventos;
}
