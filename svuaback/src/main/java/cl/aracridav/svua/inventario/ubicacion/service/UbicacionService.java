package cl.aracridav.svua.inventario.ubicacion.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import cl.aracridav.svua.inventario.ubicacion.dto.request.UbicacionCreateRequest;
import cl.aracridav.svua.inventario.ubicacion.dto.response.UbicacionResponse;

public interface UbicacionService {

    public UbicacionResponse registrarUbicacion(UbicacionCreateRequest request);
    public Page<UbicacionResponse> listarUbicaciones(Pageable pageable);

}
