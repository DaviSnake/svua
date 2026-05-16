package cl.aracridav.svua.mantenimiento.repuesto.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.empresa.repository.EmpresaRepository;
import cl.aracridav.svua.mantenimiento.repuesto.dto.request.RepuestoRequest;
import cl.aracridav.svua.mantenimiento.repuesto.dto.response.RepuestoResponse;
import cl.aracridav.svua.mantenimiento.repuesto.entity.Repuesto;
import cl.aracridav.svua.mantenimiento.repuesto.repository.RepuestoRepository;
import cl.aracridav.svua.shared.exception.BusinessException;
import cl.aracridav.svua.shared.mappers.GeneralMapper;
import cl.aracridav.svua.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RepuestoServiceImpl implements RepuestoService {

    private final RepuestoRepository repository;
    private final EmpresaRepository empresaRepository;
    private final GeneralMapper mapper;

    /*
     * =========================================
     * CREAR
     * =========================================
     */
    @Override
    public RepuestoResponse crear(RepuestoRequest request) {

        Long empresaId = resolveEmpresaId(request.getEmpresaId());

        Empresa empresa = obtenerEmpresaActual(empresaId);

        validarCodigoUnico(request.getCodigo(), empresa, null);

        Repuesto repuesto = construirRepuesto(request, empresa);

        return mapper.mapRepuestoResponse(repository.save(repuesto));
    }

    /*
     * =========================================
     * LISTAR
     * =========================================
     */
    @Override
    public Page<RepuestoResponse> listarRepuestos(Pageable pageable) {

        Page<RepuestoResponse> RepuestoResponse = null;

        Long empresaId = resolveEmpresaId(null);

        Empresa empresa = obtenerEmpresaActual(empresaId);

        if (esSuperAdmin()) {
            RepuestoResponse =  repository.findAll(pageable)
                .map(mapper::mapRepuestoResponse);
        }

        if (esAdminEmpresa()) {
            RepuestoResponse = repository.findByEmpresa(empresa, pageable)
                    .map(mapper::mapRepuestoResponse);
        }

        return RepuestoResponse;

    }

    /*
     * =========================================
     * OBTENER
     * =========================================
     */
    @Override
    public RepuestoResponse obtener(Long id) {

        return mapper.mapRepuestoResponse(obtenerRepuesto(id));
    }

    /*
     * =========================================
     * ACTUALIZAR
     * =========================================
     */
    @Override
    public RepuestoResponse actualizar(Long id, RepuestoRequest request) {

        Repuesto repuesto = obtenerRepuesto(id);

        validarCodigoUnico(request.getCodigo(), repuesto.getEmpresa(), id);

        actualizarCampos(repuesto, request);

        return mapper.mapRepuestoResponse(repository.save(repuesto));
    }

    /*
     * =========================================
     * ELIMINAR (SOFT DELETE)
     * =========================================
     */
    @Override
    public void eliminar(Long id) {

        Repuesto repuesto = obtenerRepuesto(id);

        repuesto.setActivo(false);

        repository.save(repuesto);
    }

    /*
     * =========================================
     * HELPERS
     * =========================================
     */

    private Empresa obtenerEmpresaActual(Long empresaId) {
        return empresaRepository.findById(empresaId)
                .orElseThrow(() -> new BusinessException("Empresa no encontrada"));
    }

    private Repuesto obtenerRepuesto(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException("Repuesto no encontrado"));
    }

    private void validarCodigoUnico(String codigo, Empresa empresa, Long idActual) {

        boolean existe = repository.existsByCodigoAndEmpresa(codigo, empresa);

        if (existe) {
            // 🔥 evita falso positivo en update
            if (idActual == null || 
               repository.findById(idActual)
                    .map(r -> !r.getCodigo().equals(codigo))
                    .orElse(true)) {

                throw new BusinessException("El código ya existe");
            }
        }
    }

    private Long resolveEmpresaId(Long requestEmpresaId) {
        return requestEmpresaId != null
                ? requestEmpresaId
                : SecurityUtils.getEmpresaId();
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

    private Repuesto construirRepuesto(RepuestoRequest request, Empresa empresa) {

        Repuesto repuesto = new Repuesto();

        repuesto.setCodigo(request.getCodigo());
        repuesto.setNombre(request.getNombre());
        repuesto.setDescripcion(request.getDescripcion());
        repuesto.setCostoUnitario(request.getCostoUnitario());
        repuesto.setStockMinimo(request.getStockMinimo());
        repuesto.setActivo(
            request.getActivo() != null ? request.getActivo() : true
        );
        repuesto.setEmpresa(empresa);

        return repuesto;
    }

    private void actualizarCampos(Repuesto repuesto, RepuestoRequest request) {

        repuesto.setCodigo(request.getCodigo());
        repuesto.setNombre(request.getNombre());
        repuesto.setDescripcion(request.getDescripcion());
        repuesto.setCostoUnitario(request.getCostoUnitario());
        repuesto.setStockMinimo(request.getStockMinimo());
        repuesto.setActivo(request.getActivo());
    }
}