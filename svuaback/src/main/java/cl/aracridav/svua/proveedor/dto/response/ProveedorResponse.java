package cl.aracridav.svua.proveedor.dto.response;

import cl.aracridav.svua.empresa.entity.Empresa;
import lombok.*;

@Data
@Builder
public class ProveedorResponse {

    private Long id;
    private String nombre;
    private String rut;
    private String contacto;
    private String telefono;
    private String email;
    private Empresa empresa;

}
