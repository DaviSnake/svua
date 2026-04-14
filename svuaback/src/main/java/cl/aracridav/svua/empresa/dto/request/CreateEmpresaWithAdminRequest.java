package cl.aracridav.svua.empresa.dto.request;

import cl.aracridav.svua.empresa.entity.TipoPlan;
import lombok.Data;

@Data
public class CreateEmpresaWithAdminRequest {

    // Empresa
    private String nombre;
    private String rut;
    private String emailContacto;
    private String telefono;
    private String direccion;
    private TipoPlan tipoPlan;

    // Admin inicial
    private String adminNombre;
    private String adminEmail;
    private String adminPassword;
}
