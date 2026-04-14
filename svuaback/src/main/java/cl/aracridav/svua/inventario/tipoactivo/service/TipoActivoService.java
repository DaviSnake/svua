package cl.aracridav.svua.inventario.tipoactivo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import cl.aracridav.svua.inventario.tipoactivo.dto.request.TipoActivoCreateRequest;
import cl.aracridav.svua.inventario.tipoactivo.dto.response.TipoActivoResponse;

public interface TipoActivoService {

    public TipoActivoResponse crear(Long empresaId, TipoActivoCreateRequest tipoActivoCreateRequest);
    public Page<TipoActivoResponse> listarTipoActivos(Pageable pageable);

}
