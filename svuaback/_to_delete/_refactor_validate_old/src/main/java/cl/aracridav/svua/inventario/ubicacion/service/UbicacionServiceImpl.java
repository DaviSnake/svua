package cl.aracridav.svua.inventario.ubicacion.service;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.empresa.repository.EmpresaRepository;
import cl.aracridav.svua.inventario.ubicacion.dto.request.UbicacionCreateRequest;
import cl.aracridav.svua.inventario.ubicacion.dto.response.UbicacionResponse;
import cl.aracridav.svua.inventario.ubicacion.entity.Ubicacion;
import cl.aracridav.svua.inventario.ubicacion.repository.UbicacionRepository;
import cl.aracridav.svua.shared.exception.BusinessException;
import cl.aracridav.svua.shared.mappers.GeneralMapper;
import cl.aracridav.svua.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UbicacionServiceImpl implements UbicacionService {

    private final UbicacionRepository repository;
    private final EmpresaRepository empresaRepository;
    private final GeneralMapper mapper;

    /*
     * =========================================
     * CREAR
     * =========================================
     */
    @Override
    public UbicacionResponse registrarUbicacion(UbicacionCreateRequest request) {
        
        Empresa empresa = obtenerEmpresaActual(request.getEmpresaId());

        validarRequest(request);
        validarDuplicado(request.getNombre(), empresa.getId());

        Ubicacion ubicacion = construirUbicacion(request, empresa);

        return mapper.mapUbicacionResponse(repository.save(ubicacion));
    }

    /*
     * =========================================
     * OBTENER
     * =========================================
     */
    @Override
    @Transactional(readOnly = true)
    public UbicacionResponse obtener(Long id) {

        Ubicacion ubicacion = obtenerUbicacion(id);
        Empresa empresa = obtenerEmpresaActual(ubicacion.getEmpresa().getId());

        validarPerteneceEmpresa(ubicacion, empresa.getId());

        return mapper.mapUbicacionResponse(ubicacion);
    }

    /*
     * =========================================
     * ACTUALIZAR
     * =========================================
     */
    @Override
    public UbicacionResponse actualizar(Long id, UbicacionCreateRequest request) {


        Ubicacion ubicacion = obtenerUbicacion(id);
        Empresa empresa = obtenerEmpresaActual(request.getEmpresaId());

        validarPerteneceEmpresa(ubicacion, empresa.getId());
        validarRequest(request);

        if (request.getNombre() != null &&
            !request.getNombre().equalsIgnoreCase(ubicacion.getNombre())) {

            validarDuplicado(request.getNombre(), empresa.getId());
            ubicacion.setNombre(request.getNombre());
        }

        if (request.getDescripcion() != null) {
            ubicacion.setDescripcion(request.getDescripcion());
        }

        if (request.getDireccion() != null) {
            ubicacion.setDireccion(request.getDireccion());
        }

        return mapper.mapUbicacionResponse(repository.save(ubicacion));
    }

    /*
     * =========================================
     * ELIMINAR (SOFT DELETE)
     * =========================================
     */
    @Override
    public void eliminar(Long id) {

        Ubicacion ubicacion = obtenerUbicacion(id);
        Empresa empresa = obtenerEmpresaActual(ubicacion.getEmpresa().getId());

        validarPerteneceEmpresa(ubicacion, empresa.getId());

        ubicacion.setActivo(false);

        repository.save(ubicacion);
    }

    /*
     * =========================================
     * LISTAR
     * =========================================
     */
    @Override
    @Transactional(readOnly = true)
    public Page<UbicacionResponse> listarUbicaciones(Pageable pageable) {

        Page<Ubicacion> ubicaciones = null;

        boolean esAdmin = false;

        if (esSuperAdmin()) {
            ubicaciones = repository.findAll(pageable);
            esAdmin = true;
        }

        if (esAdminEmpresa()) {
            ubicaciones = repository.findByEmpresaId(
                SecurityUtils.getEmpresaId(), pageable);
            esAdmin = true;
        }

        if (!esAdmin){
            throw new BusinessException("No tienes permisos para ver ubicaciones");
        }

        return ubicaciones.map(mapper::mapUbicacionResponse);
    }

    /*
     * =========================================
     * VALIDACIONES
     * =========================================
     */
    private void validarRequest(UbicacionCreateRequest request) {

        if (request.getNombre() == null || request.getNombre().isBlank()) {
            throw new BusinessException("El nombre de la ubicación es obligatorio");
        }
    }

    private void validarDuplicado(String nombre, Long empresaId) {

        if (repository.existsByNombreIgnoreCaseAndEmpresaId(nombre, empresaId)) {
            throw new BusinessException("Ya existe una ubicación con ese nombre");
        }
    }

    private void validarPerteneceEmpresa(Ubicacion ubicacion, Long empresaId) {
        if (!ubicacion.getEmpresa().getId().equals(empresaId)) {
            throw new BusinessException("No pertenece a la empresa");
        }
    }

    /*
     * =========================================
     * BUILDER
     * =========================================
     */
    private Ubicacion construirUbicacion(
            UbicacionCreateRequest request,
            Empresa empresa) {

        Ubicacion u = new Ubicacion();

        u.setNombre(request.getNombre());
        u.setDescripcion(request.getDescripcion());
        u.setDireccion(request.getDireccion());
        u.setEmpresa(empresa);
        u.setActivo(true);

        return u;
    }

    /*
     * =========================================
     * HELPERS
     * =========================================
     */
    private Ubicacion obtenerUbicacion(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException("Ubicación no encontrada"));
    }

    private Empresa obtenerEmpresaActual(Long empresaId) {
        return empresaRepository.findById(empresaId)
                .orElseThrow(() -> new BusinessException("Empresa no encontrada"));
    }

    

    private boolean esSuperAdmin() {
        return SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
    }

    private boolean esAdminEmpresa() {
        return SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN_EMPRESA"));
    }
}