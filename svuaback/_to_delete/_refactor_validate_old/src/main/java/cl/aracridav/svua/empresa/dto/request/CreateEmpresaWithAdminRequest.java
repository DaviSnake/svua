package cl.aracridav.svua.empresa.dto.request;

import lombok.*;

@Data
@EqualsAndHashCode(callSuper = true)
public class CreateEmpresaWithAdminRequest extends CreateEmpresaRequest {

    // Admin inicial
    private String adminNombre;
    private String adminEmail;
    private String adminPassword;
}
