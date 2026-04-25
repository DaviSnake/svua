package cl.aracridav.svua.inventario.activo.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DarDeBajaActivoRequest {

    @NotBlank(message = "El motivo es obligatorio")
    private String motivo;
}
