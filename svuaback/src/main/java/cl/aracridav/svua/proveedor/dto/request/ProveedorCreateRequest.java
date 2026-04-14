package cl.aracridav.svua.proveedor.dto.request;

import lombok.Data;

@Data
public class ProveedorCreateRequest {

    private String nombre;
    private String rut;
    private String contacto;
    private String telefono;
    private String email;

}
