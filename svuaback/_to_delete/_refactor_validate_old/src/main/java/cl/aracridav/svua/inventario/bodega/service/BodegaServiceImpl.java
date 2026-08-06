package cl.aracridav.svua.inventario.bodega.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.empresa.repository.EmpresaRepository;
import cl.aracridav.svua.inventario.bodega.dto.request.BodegaRequest;
import cl.aracridav.svua.inventario.bodega.dto.response.BodegaResponse;
import cl.aracridav.svua.inventario.bodega.entity.Bodega;
import cl.aracridav.svua.inventario.bodega.repository.BodegaRepository;
import cl.aracridav.svua.shared.exception.BusinessException;
import cl.aracridav.svua.shared.mappers.GeneralMapper;
import cl.aracridav.svua.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class BodegaServiceImpl implements BodegaService {

    private final BodegaRepository repository;
    private final EmpresaRepository empresaRepository;
    private final GeneralMapper mapper;

    /*
     * =========================================
     * CREAR
     * =========================================
     */
    @Override
    public BodegaResponse crear(BodegaRequest request) {

        Empresa empresa = obtenerEmpresaActual(request.getEmpresaId());

        validarRequest(request);
        validarDuplicado(request.getNombre(), empresa.getId());

        Bodega bodega = construirBodega(request, empresa);

        return mapper.mapBodegaResponse(repository.save(bodega));
    }

    /*
     * =========================================
     * LISTAR
     * =========================================
     */
    @Override
    @Transactional(readOnly = true)
    public Page<BodegaResponse> listar(Pageable pageable) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Page<Bodega> bodegas;

        if (tieneRol(auth, "ROLE_SUPER_ADMIN")) {
            bodegas = repository.findAll(pageable);

        } else if (tieneRol(auth, "ROLE_ADMIN_EMPRESA")) {
            bodegas = repository.findByEmpresaId(SecurityUtils.getEmpresaId(), pageable);

        } else {
            throw new BusinessException("No tienes permisos para ver bodegas");
        }

        return bodegas.map(mapper::mapBodegaResponse);
    }

    /*
     * =========================================
     * OBTENER
     * =========================================
     */
    @Override
    @Transactional(readOnly = true)
    public BodegaResponse obtener(Long id) {

        Bodega bodega = obtenerBodega(id);
        validarEmpresa(bodega);

        return mapper.mapBodegaResponse(bodega);
    }

    /*
     * =========================================
     * ACTUALIZAR
     * =========================================
     */
    @Override
    public BodegaResponse actualizar(Long id, BodegaRequest request) {

        Bodega bodega = obtenerBodega(id);

        validarEmpresa(bodega);
        validarRequest(request);
        validarDuplicadoUpdate(request.getNombre(), bodega);

        actualizarCampos(bodega, request);

        return mapper.mapBodegaResponse(repository.save(bodega));
    }

    /*
     * =========================================
     * ELIMINAR (SOFT DELETE)
     * =========================================
     */
    @Override
    public void eliminar(Long id) {

        Bodega bodega = obtenerBodega(id);

        validarEmpresa(bodega);

        if (!bodega.getActiva()) {
            throw new BusinessException("La bodega ya está inactiva");
        }

        bodega.setActiva(false);

        repository.save(bodega);
    }

    /*
     * =========================================
     * VALIDACIONES
     * =========================================
     */

    private void validarRequest(BodegaRequest request) {

        if (request.getNombre() == null || request.getNombre().isBlank()) {
            throw new BusinessException("El nombre de la bodega es obligatorio");
        }
    }

    private void validarDuplicado(String nombre, Long empresaId) {

        if (repository.existsByNombreIgnoreCaseAndEmpresaId(nombre, empresaId)) {
            throw new BusinessException("Ya existe una bodega con ese nombre");
        }
    }

    private void validarDuplicadoUpdate(String nombre, Bodega bodega) {

        boolean existe = repository.existsByNombreIgnoreCaseAndEmpresaId(nombre, bodega.getEmpresa().getId());

        if (existe && !bodega.getNombre().equalsIgnoreCase(nombre)) {
            throw new BusinessException("Ya existe una bodega con ese nombre");
        }
    }

    private void validarEmpresa(Bodega bodega) {

        Long empresaId = SecurityUtils.getEmpresaId();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean esSuperAdmin = tieneRol(auth, "ROLE_SUPER_ADMIN");

        if (!esSuperAdmin && !bodega.getEmpresa().getId().equals(empresaId)) {
            throw new BusinessException("No pertenece a tu empresa");
        }
    }

    /*
     * =========================================
     * BUILDER / UPDATE
     * =========================================
     */

    private Bodega construirBodega(BodegaRequest request, Empresa empresa) {

        Bodega b = new Bodega();

        b.setNombre(request.getNombre());
        b.setUbicacionFisica(request.getUbicacionFisica());
        b.setActiva(true);
        b.setEmpresa(empresa);

        return b;
    }

    private void actualizarCampos(Bodega bodega, BodegaRequest request) {

        bodega.setNombre(request.getNombre());
        bodega.setUbicacionFisica(request.getUbicacionFisica());
    }

    /*
     * =========================================
     * HELPERS
     * =========================================
     */

    private Bodega obtenerBodega(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException("Bodega no encontrada"));
    }

    private Empresa obtenerEmpresaActual(Long empresaId) {
        return empresaRepository.findById(empresaId)
                .orElseThrow(() -> new BusinessException("Empresa no encontrada"));
    }

    private boolean tieneRol(Authentication auth, String... roles) {
        List<String> rolesBuscados = Arrays.asList(roles);
        return auth.getAuthorities().stream()
                .anyMatch(a -> rolesBuscados.contains(a.getAuthority()));
    }
}