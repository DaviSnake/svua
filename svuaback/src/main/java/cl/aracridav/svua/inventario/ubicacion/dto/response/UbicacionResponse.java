package cl.aracridav.svua.inventario.ubicacion.dto.response;

import cl.aracridav.svua.empresa.entity.Empresa;
import lombok.*;

@Data
@Builder
public class UbicacionResponse {

    private Long id;
    private String nombre;
    private String descripcion;
    private String direccion;
    private Empresa empresa;

}
