package cl.aracridav.svua.inventario.tipoactivo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import cl.aracridav.svua.inventario.tipoactivo.dto.request.TipoActivoCreateRequest;
import cl.aracridav.svua.inventario.tipoactivo.dto.response.TipoActivoResponse;

public interface TipoActivoService {

    public TipoActivoResponse crear(TipoActivoCreateRequest request);
    public TipoActivoResponse actualizar(Long id, TipoActivoCreateRequest request);
    public void eliminar(Long id);
    public TipoActivoResponse obtener(Long id);
    public Page<TipoActivoResponse> listarTipoActivos(Pageable pageable);

}
