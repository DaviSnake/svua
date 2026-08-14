package cl.aracridav.svua.configuracion.dto.request;

import java.util.Map;

import lombok.Data;

@Data
public class ActualizarConfiguracionRequest {

    private Map<String, String> valores;

}
