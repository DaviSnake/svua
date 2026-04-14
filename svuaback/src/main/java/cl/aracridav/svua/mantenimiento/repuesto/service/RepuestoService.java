package cl.aracridav.svua.mantenimiento.repuesto.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import cl.aracridav.svua.mantenimiento.repuesto.dto.request.RepuestoRequest;
import cl.aracridav.svua.mantenimiento.repuesto.dto.response.RepuestoResponse;

public interface RepuestoService {

    RepuestoResponse crear(RepuestoRequest request);

    public Page<RepuestoResponse> listarRepuestos(Pageable pageable);

    RepuestoResponse obtener(Long id);

    RepuestoResponse actualizar(Long id, RepuestoRequest request);

    void eliminar(Long id);

}
