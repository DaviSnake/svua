package cl.aracridav.svua.empresa.dto.request;

import cl.aracridav.svua.empresa.entity.TipoPlan;
import lombok.Data;

@Data
public class CreateEmpresaRequest {

    private String nombre;
    private String rut;
    private String emailContacto;
    private String telefono;
    private String direccion;

    // Configuración inicial SaaS
    private TipoPlan tipoPlan;
}
