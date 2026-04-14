package cl.aracridav.svua.mantenimiento.plan.service;

import java.util.List;

import cl.aracridav.svua.mantenimiento.plan.dto.request.PlanMantenimientoCreateRequest;
import cl.aracridav.svua.mantenimiento.plan.dto.response.PlanMantenimientoReponse;

public interface PlanMantenimientoService {

    PlanMantenimientoReponse crear(PlanMantenimientoCreateRequest request);

    PlanMantenimientoReponse actualizar(Long id, PlanMantenimientoCreateRequest request);

    void desactivar(Long id);

    public PlanMantenimientoReponse obtener(Long id);

    public List<PlanMantenimientoReponse> listar();

    List<PlanMantenimientoReponse> obtenerPlanesVencidos();

    void procesarPlanesVencidos(); // Para scheduler

}
