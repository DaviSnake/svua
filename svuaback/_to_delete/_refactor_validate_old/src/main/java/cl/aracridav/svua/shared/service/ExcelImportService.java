package cl.aracridav.svua.shared.service;

import java.nio.file.Path;

public interface ExcelImportService {

    public void procesarAsync(Path path, String jobId, Long empresaId, Long usuarioId, String archivo);



}
