package cl.aracridav.svua.shared.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

/**
 * Deja un registro en disco, independiente por cada carga masiva (jobId),
 * de las filas que no se pudieron procesar al importar un Excel.
 *
 * El progreso en memoria (ver ImportProgressServiceImpl) se pierde si el
 * servidor se reinicia, y tampoco queda visible en ningún lado una vez que
 * se cierra la pantalla de carga masiva. Este log queda en disco para
 * poder revisar después qué falló en una carga puntual.
 *
 * 🔥 Las líneas de una carga se acumulan en memoria (por jobId) y recién
 * se escriben a disco al terminar, en finalizar() — y SOLO si hubo al
 * menos un error. Antes se escribía a disco en cada evento (registrarInicio
 * ya generaba el archivo), dejando un log por cada carga aunque hubiera
 * salido perfecta. Un archivo de texto por ejecución CON error:
 *   log/{empresaId}/{nombreEmpresa}_{archivo}_{timestamp}.txt
 */
@Service
public class ImportFileLogService {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter TS_ARCHIVO = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final Path DIRECTORIO_BASE = Paths.get("log");

    private final Map<String, List<String>> buffers = new ConcurrentHashMap<>();

    public void registrarInicio(String jobId, String archivo, int totalFilas) {
        agregar(jobId,
            "=== INICIO carga '%s' (jobId=%s) - %d filas - %s ==="
                .formatted(archivo, jobId, totalFilas, ahora()));
    }

    public void registrarError(String jobId, String archivo, int fila, String mensaje, String contenido) {
        agregar(jobId,
            "[FILA %d] %s | datos: %s".formatted(fila, mensaje, contenido));
    }

    /**
     * Cierra el log de una carga: agrega la línea de resumen y, si hubo
     * al menos un error, vuelca todo el buffer a un archivo .txt bajo
     * log/{empresaId}/. Si la carga terminó sin errores, el buffer se
     * descarta sin escribir nada a disco.
     */
    public void finalizar(
            String jobId,
            String archivo,
            Long empresaId,
            String nombreEmpresa,
            int total,
            int procesados,
            int errores,
            String estado
    ) {

        agregar(jobId,
            "=== FIN carga '%s' (jobId=%s) - estado=%s - total=%d, procesados=%d, errores=%d - %s ==="
                .formatted(archivo, jobId, estado, total, procesados, errores, ahora()));

        List<String> lineas = buffers.remove(jobId);

        if (errores <= 0 || lineas == null || lineas.isEmpty()) {
            return;
        }

        try {
            // 🔥 la carpeta lleva el id Y el nombre de la empresa (ej.
            // "2_Empresa_demo_Spa"), no solo el id — mucho mas facil de
            // ubicar a simple vista en el filesystem. El id va primero y
            // se mantiene como prefijo estable aunque la empresa cambie
            // de nombre despues (LogArchivoServiceImpl la sigue
            // encontrando por el prefijo "{empresaId}_").
            String nombreCarpetaEmpresa = empresaId + "_" + sanitizar(nombreEmpresa);
            Path directorioEmpresa = DIRECTORIO_BASE.resolve(nombreCarpetaEmpresa);
            Files.createDirectories(directorioEmpresa);

            String nombreArchivo = sanitizar(nombreEmpresa) + "_" + sanitizar(archivo)
                    + "_" + LocalDateTime.now().format(TS_ARCHIVO) + ".txt";

            Path archivoLog = directorioEmpresa.resolve(nombreArchivo);

            Files.write(
                archivoLog,
                lineas,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            );

        } catch (IOException e) {
            // 🔒 Un problema escribiendo el log NO debe interrumpir la carga
            // masiva en sí; solo se deja constancia en stderr.
            e.printStackTrace();
        }
    }

    private void agregar(String jobId, String linea) {
        buffers
            .computeIfAbsent(jobId, k -> Collections.synchronizedList(new ArrayList<>()))
            .add(linea);
    }

    // 🔒 nombre de empresa y de archivo van directo al nombre del archivo
    // en disco: se sanean para no arrastrar espacios, tildes raras desde
    // Excel, ni caracteres invalidos para un nombre de archivo.
    private String sanitizar(String valor) {
        if (valor == null || valor.isBlank()) {
            return "desconocido";
        }
        return valor.trim().replaceAll("[^a-zA-Z0-9-_]+", "_");
    }

    private String ahora() {
        return LocalDateTime.now().format(TS);
    }
}
