package cl.aracridav.svua.inventario.movimientoinventario.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MovimientoInventarioResponse {

    private Long id;
    private Long repuestoId;
    private String repuestoNombre;
    private String tipo;
    private Integer cantidad;
    private LocalDateTime fecha;
    private String referencia;

}
