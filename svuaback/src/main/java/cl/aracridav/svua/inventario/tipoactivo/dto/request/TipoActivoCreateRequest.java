package cl.aracridav.svua.inventario.tipoactivo.dto.request;

import lombok.Data;

@Data
public class TipoActivoCreateRequest {
    private String nombre;
    private String descripcion;
    private Integer vidaUtilReferencialMeses;
    private Boolean activo;
}
