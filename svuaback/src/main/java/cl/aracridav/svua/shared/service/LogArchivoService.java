package cl.aracridav.svua.shared.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import cl.aracridav.svua.shared.dto.response.LogArchivoResponse;

public interface LogArchivoService {

    Page<LogArchivoResponse> listar(Long empresaId, Pageable pageable);

    String leerContenido(Long empresaId, String nombreArchivo);
}
