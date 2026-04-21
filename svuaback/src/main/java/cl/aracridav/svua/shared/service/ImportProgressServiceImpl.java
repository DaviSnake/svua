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

    public void iniciar(String jobId, int total) {
        progreso.put(jobId, new ImportProgressDTO(total, 0, 0, "PROCESANDO", null));
    }

    public void incrementar(String jobId) {
        ImportProgressDTO p = progreso.get(jobId);
        p.setProcesados(p.getProcesados() + 1);
    }

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
        }
    }

    public void finalizar(String jobId) {
        progreso.get(jobId).setEstado("COMPLETADO");
    }

    public void finalizarConErrores(String jobId) {
        ImportProgressDTO p = progreso.get(jobId);
        if (p != null) {
            p.setEstado("COMPLETADO_CON_ERRORES");
        }
    }

    public ImportProgressDTO get(String jobId) {
        return progreso.get(jobId);
    }

}
