package cl.aracridav.svua.inventario.bodega.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import cl.aracridav.svua.inventario.bodega.dto.request.BodegaRequest;
import cl.aracridav.svua.inventario.bodega.dto.response.BodegaResponse;

public interface BodegaService {

    BodegaResponse crear(BodegaRequest request);

    Page<BodegaResponse> listar(Pageable pegable);

    BodegaResponse obtener(Long id);

    BodegaResponse actualizar(Long id, BodegaRequest request);

    void eliminar(Long id);

}
