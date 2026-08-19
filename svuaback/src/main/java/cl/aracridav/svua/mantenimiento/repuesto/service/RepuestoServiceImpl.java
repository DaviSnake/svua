package cl.aracridav.svua.mantenimiento.repuesto.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.empresa.repository.EmpresaRepository;
import cl.aracridav.svua.mantenimiento.repuesto.dto.request.RepuestoRequest;
import cl.aracridav.svua.mantenimiento.repuesto.dto.response.RepuestoResponse;
import cl.aracridav.svua.mantenimiento.repuesto.entity.Repuesto;
import cl.aracridav.svua.mantenimiento.repuesto.repository.RepuestoRepository;
import cl.aracridav.svua.notificacion.service.NotificacionService;
import cl.aracridav.svua.shared.exception.BusinessException;
import cl.aracridav.svua.shared.mappers.GeneralMapper;
import cl.aracridav.svua.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RepuestoServiceImpl implements RepuestoService {

    private final RepuestoRepository repository;
    private final EmpresaRepository empresaRepository;
    private final NotificacionService notificacionService;
    private final GeneralMapper mapper;

    /*
     * =========================================
     * CREAR
     * =========================================
     */
    @Override
    @Transactional
    public RepuestoResponse crear(RepuestoRequest request) {

        Long empresaId = resolveEmpresaId(request.getEmpresaId());

        Empresa empresa = obtenerEmpresaActual(empresaId);

        validarCodigoUnico(request.getCodigo(), empresa, null);

        Repuesto repuesto = construirRepuesto(request, empresa);

        Repuesto repuestoGuardado = repository.save(repuesto);

        notificacionService.verificarStockMinimo(repuestoGuardado);

        return mapper.mapRepuestoResponse(repuestoGuardado);

    }

    /*
     * =========================================
     * LISTAR
     * =========================================
     */
    @Override
    @Transactional(readOnly = true)
    public Page<RepuestoResponse> listarRepuestos(Pageable pageable, Long empresaId) {

        if (esSuperAdmin()) {

            // 🔥 SUPER_ADMIN puede ver todas las empresas o filtrar por una
            if (empresaId != null) {
                return repository.findByEmpresaId(empresaId, pageable)
                    .map(mapper::mapRepuestoResponse);
            }

            return repository.findAll(pageable)
                .map(mapper::mapRepuestoResponse);
        }

        // 🔒 Usuarios no SUPER_ADMIN siempre ven solo su propia empresa,
        // sin importar lo que llegue en empresaId.
        Long propiaEmpresaId = resolveEmpresaId(null);

        return repository.findByEmpresaId(propiaEmpresaId, pageable)
            .map(mapper::mapRepuestoResponse);
    }

    /*
     * =========================================
     * OBTENER
     * =========================================
     */
    @Override
    @Transactional(readOnly = true)
    public RepuestoResponse obtener(Long id) {

        return mapper.mapRepuestoResponse(obtenerRepuesto(id));
    }

    /*
     * =========================================
     * ACTUALIZAR
     * =========================================
     */
    @Override
    @Transactional
    public RepuestoResponse actualizar(Long id, RepuestoRequest request) {

        Repuesto repuesto = obtenerRepuesto(id);

        validarCodigoUnico(request.getCodigo(), repuesto.getEmpresa(), id);

        actualizarCampos(repuesto, request);

        Repuesto repuestoGuardado = repository.save(repuesto);

        notificacionService.verificarStockMinimo(repuestoGuardado);

        return mapper.mapRepuestoResponse(repuestoGuardado);
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
        Repuesto repuesto = repository.findById(id)
                .orElseThrow(() -> new BusinessException("Repuesto no encontrado"));

        // 🔐 Validación multi-tenant
        if (!repuesto.getEmpresa().getId().equals(SecurityUtils.getEmpresaId())) {
            throw new BusinessException("No pertenece a esta empresa");
        }

        return repuesto;
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

    private Repuesto construirRepuesto(RepuestoRequest request, Empresa empresa) {

        Repuesto repuesto = new Repuesto();

        repuesto.setCodigo(request.getCodigo());
        repuesto.setNombre(request.getNombre());
        repuesto.setDescripcion(request.getDescripcion());
        repuesto.setCuentaContable(request.getCuentaContable());
        repuesto.setCostoUnitario(request.getCostoUnitario());
        repuesto.setStockActual(request.getStockActual());
        repuesto.setStockMinimo(request.getStockMinimo());
        repuesto.setTipo(request.getTipoRepuesto());
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
        repuesto.setStockActual(request.getStockActual());
        repuesto.setStockMinimo(request.getStockMinimo());
        repuesto.setTipo(request.getTipoRepuesto());
        repuesto.setActivo(request.getActivo());
    }
}
