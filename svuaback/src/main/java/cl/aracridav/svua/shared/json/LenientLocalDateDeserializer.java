package cl.aracridav.svua.shared.json;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * Deserializador tolerante para {@link LocalDate}.
 *
 * Por defecto, si el JSON trae una fecha vacía o con un formato inválido
 * (ej: "", "31/13/2025", "no-es-una-fecha"), Jackson rechaza toda la
 * petición con un 400 antes de que el código de negocio llegue a
 * ejecutarse. Para el ingreso de un Activo eso es más agresivo de lo
 * necesario: en vez de bloquear el registro completo por un dato mal
 * tipeado, se asume que corresponde a "hoy" y se usa la fecha actual del
 * servidor.
 *
 * Se aplica puntualmente con `@JsonDeserialize(using = ...)` sobre el
 * campo `fechaAdquisicion` en los DTOs de ingreso de Activo (creación
 * individual e ingreso manual por grilla). No se usa como deserializador
 * global de LocalDate para no relajar la validación en otros campos de
 * fecha del sistema.
 */
public class LenientLocalDateDeserializer extends JsonDeserializer<LocalDate> {

    @Override
    public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

        String texto = p.getValueAsString();

        if (texto == null || texto.isBlank()) {
            return LocalDate.now();
        }

        try {
            return LocalDate.parse(texto.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }
}
