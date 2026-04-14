package cl.aracridav.svua.empresa.dto.request;

import lombok.Data;

@Data
public class UpdateEmpresaRequest {

    private String nombre;
    private String rut;
    private String direccion;
    private String telefono;
    private Boolean activa;
}
