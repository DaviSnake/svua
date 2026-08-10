package cl.aracridav.svua.shared.service;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import cl.aracridav.svua.shared.dto.response.ErrorFilaDTO;
import cl.aracridav.svua.shared.dto.response.ImportProgressDTO;

@Service
public class ImportProgressServiceImpl implements ImportProgressService {

    private final Map<String, ImportProgressDTO> progreso = new ConcurrentHashMap<>();

    @Override
    public void iniciar(String jobId, int total) {
        progreso.put(jobId, new ImportProgressDTO(total, 0, 0, "PROCESANDO", null));
    }

    @Override
    public void incrementar(String jobId) {
        ImportProgressDTO p = progreso.get(jobId);
        p.setProcesados(p.getProcesados() + 1);
    }

    @Override
    public void error(String jobId, int fila, String mensaje, String contenido) {

        ImportProgressDTO p = progreso.get(jobId);

        if (p != null) {

            if (p.getErroresDetalle() == null) {
                p.setErroresDetalle(new ArrayList<>());
            }

            p.setErrores(p.getErrores() + 1);

            p.getErroresDetalle().add(
                new ErrorFilaDTO(fila, mensaje, contenido)
            );

            // 🔒 OJO: acá NO se marca el job como "ERROR". Esto se llama
            // una vez por CADA fila que falla, mientras el resto de la
            // carga sigue procesándose en el loop. Si se pisaba el estado
            // acá, el frontend (que corta el polling en cuanto ve un
            // estado terminal) daba por terminada la carga completa en
            // la primera fila con error, aunque el backend siguiera
            // procesando el resto — el usuario nunca veía el resultado
            // final ni el resto de las filas fallidas. El estado "final"
            // (COMPLETADO / COMPLETADO_CON_ERRORES) lo define únicamente
            // finalizar()/finalizarConErrores() al terminar todo el loop.
        }
    }

    @Override
    public void finalizar(String jobId) {
        progreso.get(jobId).setEstado("COMPLETADO");
    }

    @Override
    public void finalizarConErrores(String jobId) {
        ImportProgressDTO p = progreso.get(jobId);
        if (p != null) {
            p.setEstado("COMPLETADO_CON_ERRORES");
        }
    }

    @Override
    public ImportProgressDTO get(String jobId) {
        return progreso.get(jobId);
    }

}
