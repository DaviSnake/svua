package cl.aracridav.svua.controlturno.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import cl.aracridav.svua.controlturno.dto.request.PuntoControlRequest;
import cl.aracridav.svua.controlturno.dto.response.PuntoControlResponse;

public interface PuntoControlService {

    PuntoControlResponse registrar(PuntoControlRequest request);
    PuntoControlResponse actualizar(Long id, PuntoControlRequest request);
    void eliminar(Long id);
    PuntoControlResponse obtener(Long id);
    Page<PuntoControlResponse> listar(Pageable pageable, Long empresaId, String busqueda);
    List<PuntoControlResponse> listarActivos();
}
