package cl.aracridav.svua.shared.service;

import cl.aracridav.svua.shared.dto.response.ImportProgressDTO;

public interface ImportProgressService {

     public void iniciar(String jobId, int total);

     public void incrementar(String jobId);

     public void error(String jobId, int fila, String mensaje, String contenido);

     public void finalizar(String jobId);

     public void finalizarConErrores(String jobId);

      public ImportProgressDTO get(String jobId);

}
