package cl.aracridav.svua.inventario.tipoactivo.service;

import java.util.Arrays;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.empresa.repository.EmpresaRepository;
import cl.aracridav.svua.inventario.tipoactivo.dto.request.TipoActivoCreateRequest;
import cl.aracridav.svua.inventario.tipoactivo.dto.response.TipoActivoResponse;
import cl.aracridav.svua.inventario.tipoactivo.entity.TipoActivo;
import cl.aracridav.svua.inventario.tipoactivo.repository.TipoActivoRepository;
import cl.aracridav.svua.shared.exception.BusinessException;
import cl.aracridav.svua.shared.mappers.GeneralMapper;
import cl.aracridav.svua.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TipoActivoServiceImpl implements TipoActivoService {

    private final TipoActivoRepository repository;
    private final EmpresaRepository empresaRepository;
    private final GeneralMapper mapper;

    /*
     * =========================================
     * CREAR
     * =========================================
     */
    @Override
    public TipoActivoResponse crear(TipoActivoCreateRequest request) {

        Empresa empresa = obtenerEmpresaActual();

        validarNombreUnico(request.getNombre(), empresa.getId());

        TipoActivo tipoActivo = construirTipoActivo(request, empresa);

        return mapper.mapTipoActivoResponse(repository.save(tipoActivo));
    }

    /*
     * =========================================
     * ACTUALIZAR
     * =========================================
     */
    @Override
    public TipoActivoResponse actualizar(Long id, TipoActivoCreateRequest request) {

        TipoActivo tipo = obtenerTipoActivo(id);
        Empresa empresa = obtenerEmpresaActual();

        validarPerteneceEmpresa(tipo, empresa.getId());

        if (request.getNombre() != null &&
            !request.getNombre().equalsIgnoreCase(tipo.getNombre())) {

            validarNombreUnico(request.getNombre(), empresa.getId());
            tipo.setNombre(request.getNombre());
        }

        if (request.getDescripcion() != null) {
            tipo.setDescripcion(request.getDescripcion());
        }

        if (request.getVidaUtilReferencialMeses() != null) {
            tipo.setVidaUtilReferencialMeses(request.getVidaUtilReferencialMeses());
        }

        if (request.getActivo() != null) {
            tipo.setActivo(request.getActivo());
        }

        return mapper.mapTipoActivoResponse(repository.save(tipo));
    }

    /*
     * =========================================
     * ELIMINAR (SOFT DELETE)
     * =========================================
     */
    @Override
    public void eliminar(Long id) {

        TipoActivo tipo = obtenerTipoActivo(id);
        Empresa empresa = obtenerEmpresaActual();

        validarPerteneceEmpresa(tipo, empresa.getId());

        tipo.setActivo(false);

        repository.save(tipo);
    }

    /*
     * =========================================
     * OBTENER
     * =========================================
     */
    @Override
    @Transactional(readOnly = true)
    public TipoActivoResponse obtener(Long id) {

        TipoActivo tipo = obtenerTipoActivo(id);
        Empresa empresa = obtenerEmpresaActual();

        validarPerteneceEmpresa(tipo, empresa.getId());

        return mapper.mapTipoActivoResponse(tipo);
    }

    /*
     * =========================================
     * LISTAR
     * =========================================
     */
    @Override
    @Transactional(readOnly = true)
    public Page<TipoActivoResponse> listarTipoActivos(Pageable pageable) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Page<TipoActivo> tipos;

        if (tieneRol(auth, "ROLE_SUPER_ADMIN")) {
            tipos = repository.findAll(pageable);

        } else if (tieneRol(auth, "ROLE_ADMIN_EMPRESA")) {
            tipos = repository.findByEmpresaId(SecurityUtils.getEmpresaId(), pageable);

        } else {
            throw new BusinessException("No tienes permisos para ver tipos activos");
        }

        return tipos.map(mapper::mapTipoActivoResponse);
    }

    /*
     * =========================================
     * BUILDER
     * =========================================
     */
    private TipoActivo construirTipoActivo(TipoActivoCreateRequest request, Empresa empresa) {

        TipoActivo t = new TipoActivo();

        t.setNombre(request.getNombre());
        t.setDescripcion(request.getDescripcion());
        t.setVidaUtilReferencialMeses(request.getVidaUtilReferencialMeses());
        t.setActivo(request.getActivo() != null ? request.getActivo() : true);
        t.setEmpresa(empresa);

        return t;
    }

    /*
     * =========================================
     * HELPERS
     * =========================================
     */

    private TipoActivo obtenerTipoActivo(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException("Tipo de activo no encontrado"));
    }

    private Empresa obtenerEmpresaActual() {
        return empresaRepository.findById(SecurityUtils.getEmpresaId())
                .orElseThrow(() -> new BusinessException("Empresa no encontrada"));
    }

    private void validarNombreUnico(String nombre, Long empresaId) {
        if (repository.existsByNombreIgnoreCaseAndEmpresaId(nombre, empresaId)) {
            throw new BusinessException("Ya existe un tipo de activo con ese nombre");
        }
    }

    private void validarPerteneceEmpresa(TipoActivo tipo, Long empresaId) {
        if (!tipo.getEmpresa().getId().equals(empresaId)) {
            throw new BusinessException("No pertenece a la empresa");
        }
    }

    private boolean tieneRol(Authentication auth, String... roles) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> Arrays.asList(roles).contains(a.getAuthority()));
    }
}