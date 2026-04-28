package cl.aracridav.svua.proveedor.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.empresa.repository.EmpresaRepository;
import cl.aracridav.svua.proveedor.dto.request.ProveedorCreateRequest;
import cl.aracridav.svua.proveedor.dto.request.ProveedorUpdateRequest;
import cl.aracridav.svua.proveedor.dto.response.ProveedorResponse;
import cl.aracridav.svua.proveedor.entity.Proveedor;
import cl.aracridav.svua.proveedor.repository.ProveedorRepository;
import cl.aracridav.svua.shared.exception.BusinessException;
import cl.aracridav.svua.shared.mappers.GeneralMapper;
import cl.aracridav.svua.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ProveedorServiceImpl implements ProveedorService {

    private final ProveedorRepository proveedorRepository;
    private final EmpresaRepository empresaRepository;
    private final GeneralMapper mapper;

    /*
     * =========================================
     * CREAR
     * =========================================
     */
    @Override
    public ProveedorResponse registrarProveedor(ProveedorCreateRequest request) {

        validarRequest(request);

        Empresa empresa = obtenerEmpresaActual();

        validarRutUnico(request.getRut(), empresa.getId());

        Proveedor proveedor = construirProveedor(request, empresa);

        return mapper.mapProeedorResponse(
                proveedorRepository.save(proveedor)
        );
    }

    /*
     * =========================================
     * LISTAR
     * =========================================
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ProveedorResponse> listarProveedores(Pageable pageable){

        if (esSuperAdmin()) {
            return proveedorRepository.findAll(pageable)
                    .map(mapper::mapProeedorResponse);
        }

        Empresa empresa = obtenerEmpresaActual();

        return proveedorRepository
                .findByEmpresaId(empresa.getId(), pageable)
                .map(mapper::mapProeedorResponse);
    }

    /*
     * =========================================
     * OBTENER
     * =========================================
     */
    @Override
    @Transactional(readOnly = true)
    public ProveedorResponse obtener(Long id) {

        Proveedor proveedor = obtenerProveedor(id);
        validarEmpresa(proveedor);

        return mapper.mapProeedorResponse(proveedor);
    }

    /*
     * =========================================
     * ACTUALIZAR
     * =========================================
     */
    @Override
    public ProveedorResponse actualizar(Long id, ProveedorUpdateRequest request) {

        validarRequest(request);

        Proveedor proveedor = obtenerProveedor(id);
        validarEmpresa(proveedor);

        validarRutUnicoUpdate(request.getRut(), proveedor);

        actualizarCampos(proveedor, request);

        return mapper.mapProeedorResponse(
                proveedorRepository.save(proveedor)
        );
    }

    /*
     * =========================================
     * ELIMINAR (SOFT DELETE)
     * =========================================
     */
    @Override
    public void eliminar(Long id) {

        Proveedor proveedor = obtenerProveedor(id);
        validarEmpresa(proveedor);

        if (!proveedor.getActivo()) {
            throw new BusinessException("El proveedor ya está inactivo");
        }

        proveedor.setActivo(false);

        proveedorRepository.save(proveedor);
    }

    /*
     * =========================================
     * VALIDACIONES
     * =========================================
     */

    private void validarRequest(ProveedorUpdateRequest request) {

        if (isBlank(request.getNombre())) {
            throw new BusinessException("El nombre del proveedor es obligatorio");
        }

        if (isBlank(request.getRut())) {
            throw new BusinessException("El RUT del proveedor es obligatorio");
        }
    }

    private void validarRequest(ProveedorCreateRequest request) {

        if (isBlank(request.getNombre())) {
            throw new BusinessException("El nombre del proveedor es obligatorio");
        }

        if (isBlank(request.getRut())) {
            throw new BusinessException("El RUT del proveedor es obligatorio");
        }
    }

    private void validarRutUnico(String rut, Long empresaId) {

        boolean existe = proveedorRepository
                .existsByRutAndEmpresaId(rut, empresaId);

        if (existe) {
            throw new BusinessException("Ya existe un proveedor con ese RUT");
        }
    }

    private void validarRutUnicoUpdate(String rut, Proveedor proveedor) {

        boolean existe = proveedorRepository
                .existsByRutAndEmpresaId(rut, proveedor.getEmpresa().getId());

        if (existe && !proveedor.getRut().equalsIgnoreCase(rut)) {
            throw new BusinessException("Ya existe un proveedor con ese RUT");
        }
    }

    private void validarEmpresa(Proveedor proveedor) {

        if (esSuperAdmin()) return;

        Long empresaId = SecurityUtils.getEmpresaId();

        if (!proveedor.getEmpresa().getId().equals(empresaId)) {
            throw new BusinessException("No pertenece a tu empresa");
        }
    }

    /*
     * =========================================
     * BUILDER / UPDATE
     * =========================================
     */

    private Proveedor construirProveedor(
            ProveedorCreateRequest request,
            Empresa empresa) {

        Proveedor proveedor = new Proveedor();

        proveedor.setNombre(request.getNombre());
        proveedor.setRut(request.getRut());
        proveedor.setContacto(request.getContacto());
        proveedor.setTelefono(request.getTelefono());
        proveedor.setEmail(request.getEmail());
        proveedor.setEmpresa(empresa);
        proveedor.setActivo(true);

        return proveedor;
    }

    private void actualizarCampos(Proveedor proveedor, ProveedorUpdateRequest request) {

        proveedor.setNombre(request.getNombre());
        proveedor.setRut(request.getRut());
        proveedor.setContacto(request.getContacto());
        proveedor.setTelefono(request.getTelefono());
        proveedor.setEmail(request.getEmail());
    }

    /*
     * =========================================
     * HELPERS
     * =========================================
     */

    private Proveedor obtenerProveedor(Long id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Proveedor no encontrado"));
    }

    private Empresa obtenerEmpresaActual() {
        return empresaRepository.findById(SecurityUtils.getEmpresaId())
                .orElseThrow(() -> new BusinessException("Empresa no encontrada"));
    }

    private boolean esSuperAdmin() {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}