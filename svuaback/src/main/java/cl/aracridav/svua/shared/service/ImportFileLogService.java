package cl.aracridav.svua.shared.service;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
 * Un archivo de texto por carga: logs/carga-masiva/{archivo}_{jobId}.log
 */
@Service
public class ImportFileLogService {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Path DIRECTORIO = Paths.get("logs", "carga-masiva");

    public void registrarInicio(String jobId, String archivo, int totalFilas) {
        escribir(jobId, archivo,
            "=== INICIO carga '%s' (jobId=%s) - %d filas - %s ==="
                .formatted(archivo, jobId, totalFilas, ahora()));
    }

    public void registrarError(String jobId, String archivo, int fila, String mensaje, String contenido) {
        escribir(jobId, archivo,
            "[FILA %d] %s | datos: %s".formatted(fila, mensaje, contenido));
    }

    public void registrarResumen(String jobId, String archivo, int total, int procesados, int errores, String estado) {
        escribir(jobId, archivo,
            "=== FIN carga '%s' (jobId=%s) - estado=%s - total=%d, procesados=%d, errores=%d - %s ==="
                .formatted(archivo, jobId, estado, total, procesados, errores, ahora()));
    }

    private String ahora() {
        return LocalDateTime.now().format(TS);
    }

    private synchronized void escribir(String jobId, String archivo, String linea) {
        try {
            Files.createDirectories(DIRECTORIO);

            Path archivoLog = DIRECTORIO.resolve(archivo + "_" + jobId + ".log");

            try (PrintWriter writer = new PrintWriter(
                    Files.newBufferedWriter(
                        archivoLog,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND))) {

                writer.println(linea);
            }

        } catch (IOException e) {
            // 🔒 Un problema escribiendo el log NO debe interrumpir la carga
            // masiva en sí; solo se deja constancia en stderr.
            e.printStackTrace();
        }
    }
}
