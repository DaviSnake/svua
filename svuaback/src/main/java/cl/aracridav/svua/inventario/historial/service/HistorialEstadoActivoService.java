package cl.aracridav.svua.inventario.historial.service;

import java.util.List;

import cl.aracridav.svua.inventario.historial.dto.response.HistorialEstadoActivoResponse;
import cl.aracridav.svua.shared.enums.EstadoActivo;

public interface HistorialEstadoActivoService {

    void registrarCambioEstado(
            Long activoId,
            EstadoActivo nuevoEstado,
            String comentario
    );

    List<HistorialEstadoActivoResponse> obtenerHistorial(Long activoId);

    HistorialEstadoActivoResponse obtenerUltimoEstado(Long activoId);

    List<HistorialEstadoActivoResponse> obtenerPorEstado(
            Long activoId,
            EstadoActivo estado
    );

}
