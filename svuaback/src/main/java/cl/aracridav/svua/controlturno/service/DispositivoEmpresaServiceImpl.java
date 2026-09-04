package cl.aracridav.svua.controlturno.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.aracridav.svua.controlturno.dto.request.DispositivoEmpresaRequest;
import cl.aracridav.svua.controlturno.dto.response.DispositivoEmpresaResponse;
import cl.aracridav.svua.controlturno.entity.DispositivoEmpresa;
import cl.aracridav.svua.controlturno.repository.DispositivoEmpresaRepository;
import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.empresa.repository.EmpresaRepository;
import cl.aracridav.svua.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;

// 🔥 CRUD del catalogo dispositivo -> empresa (ver DispositivoEmpresa).
// A diferencia de PuntoControl, esto es exclusivo de SUPER_ADMIN (ver
// DispositivoEmpresaController): que dispositivo fisico alimenta a que
// empresa lo define quien instala el sensor, no cada empresa por su
// cuenta.
@Service
@RequiredArgsConstructor
@Transactional
public class DispositivoEmpresaServiceImpl implements DispositivoEmpresaService {

    private final DispositivoEmpresaRepository repository;
    private final EmpresaRepository empresaRepository;

    @Override
    public DispositivoEmpresaResponse registrar(DispositivoEmpresaRequest request) {

        validarRequest(request);
        validarDuplicado(request.getCodigoDispositivo());

        Empresa empresa = obtenerEmpresa(request.getEmpresaId());

        DispositivoEmpresa dispositivo = new DispositivoEmpresa();
        dispositivo.setCodigoDispositivo(request.getCodigoDispositivo().trim());
        dispositivo.setDescripcion(request.getDescripcion());
        dispositivo.setEmpresa(empresa);
        dispositivo.setActivo(true);

        return mapResponse(repository.save(dispositivo));
    }

    @Override
    @Transactional(readOnly = true)
    public DispositivoEmpresaResponse obtener(Long id) {
        return mapResponse(obtenerDispositivo(id));
    }

    @Override
    public DispositivoEmpresaResponse actualizar(Long id, DispositivoEmpresaRequest request) {

        validarRequest(request);

        DispositivoEmpresa dispositivo = obtenerDispositivo(id);

        String nuevoCodigo = request.getCodigoDispositivo().trim();
        if (!nuevoCodigo.equalsIgnoreCase(dispositivo.getCodigoDispositivo())) {
            validarDuplicado(nuevoCodigo);
            dispositivo.setCodigoDispositivo(nuevoCodigo);
        }

        dispositivo.setDescripcion(request.getDescripcion());
        dispositivo.setEmpresa(obtenerEmpresa(request.getEmpresaId()));

        return mapResponse(repository.save(dispositivo));
    }

    @Override
    public void eliminar(Long id) {
        DispositivoEmpresa dispositivo = obtenerDispositivo(id);
        dispositivo.setActivo(false);
        repository.save(dispositivo);
    }

    @Override
    public void habilitar(Long id) {
        DispositivoEmpresa dispositivo = obtenerDispositivo(id);
        dispositivo.setActivo(true);
        repository.save(dispositivo);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DispositivoEmpresaResponse> listar(Pageable pageable, Long empresaId, String busqueda) {
        return repository.buscarDispositivos(empresaId, busqueda, pageable).map(this::mapResponse);
    }

    private void validarRequest(DispositivoEmpresaRequest request) {

        if (request.getCodigoDispositivo() == null || request.getCodigoDispositivo().isBlank()) {
            throw new BusinessException("El código del dispositivo es obligatorio");
        }

        if (request.getEmpresaId() == null) {
            throw new BusinessException("Debe seleccionar una empresa");
        }
    }

    private void validarDuplicado(String codigoDispositivo) {
        if (repository.existsByCodigoDispositivoIgnoreCase(codigoDispositivo)) {
            throw new BusinessException("Ya existe un dispositivo registrado con ese código");
        }
    }

    private DispositivoEmpresa obtenerDispositivo(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException("Dispositivo no encontrado"));
    }

    private Empresa obtenerEmpresa(Long empresaId) {
        return empresaRepository.findById(empresaId)
                .orElseThrow(() -> new BusinessException("Empresa no encontrada"));
    }

    private DispositivoEmpresaResponse mapResponse(DispositivoEmpresa d) {
        return DispositivoEmpresaResponse.builder()
                .id(d.getId())
                .codigoDispositivo(d.getCodigoDispositivo())
                .descripcion(d.getDescripcion())
                .activo(d.getActivo())
                .empresaId(d.getEmpresa().getId())
                .empresaNombre(d.getEmpresa().getNombre())
                .build();
    }
}
