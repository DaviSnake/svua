package cl.aracridav.svua.proveedor.dto.response;

import cl.aracridav.svua.proveedor.entity.TipoProveedor;
import cl.aracridav.svua.shared.dto.response.EmpresaDTO;
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
    private Boolean activo;
    private TipoProveedor tipoProveedor;
    private EmpresaDTO empresa;

}
