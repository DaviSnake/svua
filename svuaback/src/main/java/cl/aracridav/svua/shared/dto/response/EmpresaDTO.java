package cl.aracridav.svua.shared.dto.response;

import cl.aracridav.svua.empresa.entity.TipoPlan;
import lombok.*;

@Data
@Builder
public class EmpresaDTO {

    private Long id;
    private String nombre;
    private String rut;
    private String emailContacto;
    private String telefono;
    private TipoPlan tipoPlan;

}
