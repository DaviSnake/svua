package cl.aracridav.svua.inventario.activo.dto.response;

import lombok.*;

@Data
@Builder
public class TipoActivoDTO {

    private Long id;
    private String nombre;
    private Integer vidaUtilReferencialMeses;

}
