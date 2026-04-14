package cl.aracridav.svua.inventario.tipoactivo.dto.response;

import cl.aracridav.svua.empresa.entity.Empresa;
import lombok.*;

@Data
@Builder
public class TipoActivoResponse {

    private Long id;
    private String nombre;
    private String descripcion;
    private Integer vidaUtilReferencialMeses;
    private Empresa empresa;

}
