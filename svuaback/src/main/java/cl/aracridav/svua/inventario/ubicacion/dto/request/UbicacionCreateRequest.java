package cl.aracridav.svua.inventario.ubicacion.dto.request;

import lombok.Data;

@Data
public class UbicacionCreateRequest {

    private String nombre;
    private String descripcion;
    private String direccion;

}
