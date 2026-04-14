package cl.aracridav.svua.inventario.activo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import cl.aracridav.svua.inventario.activo.dto.request.ActivoCreateRequest;
import cl.aracridav.svua.inventario.activo.dto.response.ActivoResponse;
import cl.aracridav.svua.inventario.activo.entity.Activo;
import cl.aracridav.svua.shared.enums.EstadoActivo;

public interface ActivoService {

    ActivoResponse crearActivo(Long empresaId, ActivoCreateRequest nuevoEstado);
    Page<ActivoResponse> mostrarActivos(Pageable pageable);
    Activo darDeBaja(Long idActivo, String motivo);
    void actualizarEstado(Long idActivo, EstadoActivo estado);

}
