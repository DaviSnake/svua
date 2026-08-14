package cl.aracridav.svua.configuracion.service;

import java.util.List;
import java.util.Map;

import cl.aracridav.svua.configuracion.dto.response.ConfiguracionEntryResponse;

public interface ConfiguracionService {

    List<ConfiguracionEntryResponse> leerConfiguracion();

    void actualizarConfiguracion(Map<String, String> valores);

}
