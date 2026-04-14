package cl.aracridav.svua.proveedor.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import cl.aracridav.svua.proveedor.dto.request.ProveedorCreateRequest;
import cl.aracridav.svua.proveedor.dto.response.ProveedorResponse;

public interface ProveedorService {

    public ProveedorResponse registrarProveedor(ProveedorCreateRequest request);
    public Page<ProveedorResponse> listarProveedores(Pageable pageable);

}
