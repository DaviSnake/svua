package cl.aracridav.svua.configuracion.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionEntryResponse {

    private String clave;
    private String valor;

}
