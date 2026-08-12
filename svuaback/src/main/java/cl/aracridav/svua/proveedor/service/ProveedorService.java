package cl.aracridav.svua.proveedor.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import cl.aracridav.svua.proveedor.dto.request.ProveedorCreateRequest;
import cl.aracridav.svua.proveedor.dto.request.ProveedorUpdateRequest;
import cl.aracridav.svua.proveedor.dto.response.ProveedorResponse;

public interface ProveedorService {

    public ProveedorResponse registrarProveedor(ProveedorCreateRequest request);
    public ProveedorResponse actualizar(Long id, ProveedorUpdateRequest request);
    public void eliminar(Long id);
    public ProveedorResponse obtener(Long id);
    public Page<ProveedorResponse> listarProveedores(Pageable pageable, Long empresaId);

}
