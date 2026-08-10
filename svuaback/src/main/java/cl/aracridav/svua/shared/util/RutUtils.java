package cl.aracridav.svua.shared.util;

/**
 * Utilidades para normalizar el RUT chileno antes de guardarlo en la base
 * de datos.
 *
 * El usuario puede ingresar el RUT con puntos de separador de miles (ej:
 * "12.345.678-9"). Se guarda siempre sin puntos (ej: "12345678-9") para que
 * las validaciones de unicidad y las búsquedas no dependan de si alguien
 * escribió los puntos o no.
 */
public final class RutUtils {

    private RutUtils() {
    }

    public static String limpiarRut(String rut) {

        if (rut == null) {
            return null;
        }

        return rut.replace(".", "").trim();
    }
}
