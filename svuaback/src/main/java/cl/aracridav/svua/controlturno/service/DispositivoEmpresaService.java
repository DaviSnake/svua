package cl.aracridav.svua.controlturno.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import cl.aracridav.svua.controlturno.dto.request.DispositivoEmpresaRequest;
import cl.aracridav.svua.controlturno.dto.response.DispositivoEmpresaResponse;

public interface DispositivoEmpresaService {

    DispositivoEmpresaResponse registrar(DispositivoEmpresaRequest request);

    DispositivoEmpresaResponse obtener(Long id);

    DispositivoEmpresaResponse actualizar(Long id, DispositivoEmpresaRequest request);

    void eliminar(Long id);

    void habilitar(Long id);

    Page<DispositivoEmpresaResponse> listar(Pageable pageable, Long empresaId, String busqueda);
}
