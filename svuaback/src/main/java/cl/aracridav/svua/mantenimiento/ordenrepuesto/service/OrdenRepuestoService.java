package cl.aracridav.svua.mantenimiento.ordenrepuesto.service;

import java.util.List;

import cl.aracridav.svua.mantenimiento.ordenrepuesto.dto.request.OrdenRepuestoRequest;
import cl.aracridav.svua.mantenimiento.ordenrepuesto.dto.response.OrdenRepuestoResponse;

public interface OrdenRepuestoService {

    public OrdenRepuestoResponse agregarRepuesto(OrdenRepuestoRequest request);
    public List<OrdenRepuestoResponse> listarPorOrden(Long ordenId);

}
