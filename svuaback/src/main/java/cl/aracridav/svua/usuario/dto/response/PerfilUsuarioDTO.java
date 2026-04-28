package cl.aracridav.svua.usuario.dto.response;

import java.time.LocalDate;

import lombok.*;

@Data
public class PerfilUsuarioDTO {

    private Long id;
    private String nombre;
    private String email;
    private String rol;
    private Boolean activo;

    // Empresa (solo lo importante)
    private String empresaNombre;
    private String empresaRut;
    private String plan;
    private LocalDate fechaFinPlan;

}
