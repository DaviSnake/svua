package cl.aracridav.svua.inventario.activo.dto.response;

import lombok.*;

@Data
@Builder
public class ProveedorDTO {

    private Long id;
    private String nombre;
    private String contacto;

}
