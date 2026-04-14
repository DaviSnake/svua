package cl.aracridav.svua.inventario.movimientoinventario.dto.request;

import lombok.Data;

@Data
public class MovimientoInventarioRequest {

    private Long bodegaId;
    private Long repuestoId;
    private String tipo;
    private Integer cantidad;
    private String referencia;
    private String motovo;

}
