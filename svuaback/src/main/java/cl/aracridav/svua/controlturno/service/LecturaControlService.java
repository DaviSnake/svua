package cl.aracridav.svua.controlturno.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import cl.aracridav.svua.controlturno.dto.request.LecturaControlRequest;
import cl.aracridav.svua.controlturno.dto.response.LecturaControlResponse;
import cl.aracridav.svua.controlturno.dto.response.PuntoControlDashboardResponse;
import cl.aracridav.svua.controlturno.enums.TurnoTrabajo;

public interface LecturaControlService {

    LecturaControlResponse registrar(LecturaControlRequest request);

    Page<LecturaControlResponse> listar(
            Pageable pageable, Long puntoControlId,
            LocalDateTime desde, LocalDateTime hasta, TurnoTrabajo turno);

    List<PuntoControlDashboardResponse> dashboard(
            Long puntoControlId, LocalDateTime desde, LocalDateTime hasta, TurnoTrabajo turno);
}
