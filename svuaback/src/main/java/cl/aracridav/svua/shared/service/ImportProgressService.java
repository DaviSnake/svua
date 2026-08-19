package cl.aracridav.svua.shared.service;

import cl.aracridav.svua.shared.dto.response.ImportProgressDTO;

public interface ImportProgressService {

     public void iniciar(String jobId, int total);

     public void incrementar(String jobId);

     // 🔐 Incrementa el contador de procesados de una sola vez, en la
     // cantidad indicada, en vez de fila por fila — usado por el guardado
     // por lotes (ver ImportBatchPersistenceService) para que el progreso
     // solo avance DESPUES de que un lote realmente hizo commit, y no
     // apenas se mapea cada fila en memoria.
     public void incrementarEnLote(String jobId, int cantidad);

     public void error(String jobId, int fila, String mensaje, String contenido);

     public void finalizar(String jobId);

     public void finalizarConErrores(String jobId);

      public ImportProgressDTO get(String jobId);

}
