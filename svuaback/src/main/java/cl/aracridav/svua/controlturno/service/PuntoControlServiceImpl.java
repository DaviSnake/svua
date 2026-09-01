package cl.aracridav.svua.controlturno.service;

import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.aracridav.svua.controlturno.dto.request.PuntoControlRequest;
import cl.aracridav.svua.controlturno.dto.response.PuntoControlResponse;
import cl.aracridav.svua.controlturno.entity.PuntoControl;
import cl.aracridav.svua.controlturno.repository.PuntoControlRepository;
import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.empresa.repository.EmpresaRepository;
import cl.aracridav.svua.shared.exception.BusinessException;
import cl.aracridav.svua.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;

// 🔥 CRUD del catalogo de puntos de control (ver PuntoControl). Mismo
// patron que UbicacionServiceImpl: SUPER_ADMIN administra cualquier
// empresa, ADMIN_EMPRESA solo la propia.
@Service
@RequiredArgsConstructor
@Transactional
public class PuntoControlServiceImpl implements PuntoControlService {

    private final PuntoControlRepository repository;
    private final EmpresaRepository empresaRepository;

    @Override
    public PuntoControlResponse registrar(PuntoControlRequest request) {

        validarControlTurnoHabilitado();

        Empresa empresa = obtenerEmpresaActual(request.getEmpresaId());

        validarRequest(request);
        validarDuplicado(request.getNombre(), empresa.getId());

        PuntoControl puntoControl = construir(request, empresa);

        return mapResponse(repository.save(puntoControl));
    }

    @Override
    @Transactional(readOnly = true)
    public PuntoControlResponse obtener(Long id) {

        validarControlTurnoHabilitado();

        PuntoControl puntoControl = obtenerPuntoControl(id);

        validarPerteneceEmpresaActual(puntoControl);

        return mapResponse(puntoControl);
    }

    @Override
    public PuntoControlResponse actualizar(Long id, PuntoControlRequest request) {

        validarControlTurnoHabilitado();

        PuntoControl puntoControl = obtenerPuntoControl(id);

        validarPerteneceEmpresaActual(puntoControl);
        validarRequest(request);

        if (request.getNombre() != null &&
                !request.getNombre().equalsIgnoreCase(puntoControl.getNombre())) {

            validarDuplicado(request.getNombre(), puntoControl.getEmpresa().getId());
            puntoControl.setNombre(request.getNombre());
        }

        puntoControl.setUnidad(request.getUnidad());
        puntoControl.setValorMin(request.getValorMin());
        puntoControl.setValorMax(request.getValorMax());

        return mapResponse(repository.save(puntoControl));
    }

    @Override
    public void eliminar(Long id) {

        validarControlTurnoHabilitado();

        PuntoControl puntoControl = obtenerPuntoControl(id);

        validarPerteneceEmpresaActual(puntoControl);

        puntoControl.setActivo(false);

        repository.save(puntoControl);
    }

    @Override
    public void habilitar(Long id) {

        validarControlTurnoHabilitado();

        PuntoControl puntoControl = obtenerPuntoControl(id);

        validarPerteneceEmpresaActual(puntoControl);

        puntoControl.setActivo(true);

        repository.save(puntoControl);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PuntoControlResponse> listar(Pageable pageable, Long empresaId, String busqueda) {

        validarControlTurnoHabilitado();

        Page<PuntoControl> puntos;

        if (esSuperAdmin()) {
            // 🔥 SUPER_ADMIN puede ver todas las empresas o filtrar por
            // una; empresaId == null equivale a "todas".
            puntos = repository.buscarPuntosControl(empresaId, busqueda, pageable);
        } else if (esAdminEmpresa()) {
            // 🔒 ADMIN_EMPRESA siempre ve solo su propia empresa.
            puntos = repository.buscarPuntosControl(SecurityUtils.getEmpresaId(), busqueda, pageable);
        } else {
            throw new BusinessException("No tienes permisos para ver puntos de control");
        }

        return puntos.map(this::mapResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PuntoControlResponse> listarActivos() {

        validarControlTurnoHabilitado();

        // 🔥 Combo simple para el formulario de ingreso de lecturas:
        // cualquier rol que registra datos de turno ve los puntos
        // activos de su propia empresa (no hay variante "todas las
        // empresas" aqui, a diferencia de listar()).
        return repository.findByEmpresaIdAndActivoTrue(SecurityUtils.getEmpresaId())
                .stream()
                .map(this::mapResponse)
                .toList();
    }

    // 🔒 Defensa en profundidad (V33): el sidebar y el guard de rutas del
    // frontend ya ocultan/bloquean Control de Turno si la empresa no lo
    // tiene habilitado, pero eso no impide una llamada directa a la API.
    // SUPER_ADMIN queda exento, igual que con codigoQrHabilitado/
    // codigoEan13Habilitado (ver SecurityUtils).
    private void validarControlTurnoHabilitado() {
        if (!esSuperAdmin() && !SecurityUtils.tieneControlTurnoHabilitado()) {
            throw new BusinessException("Control de Turno no está habilitado para su empresa");
        }
    }

    private void validarRequest(PuntoControlRequest request) {

        if (request.getNombre() == null || request.getNombre().isBlank()) {
            throw new BusinessException("El nombre del punto de control es obligatorio");
        }

        if (request.getUnidad() == null || request.getUnidad().isBlank()) {
            throw new BusinessException("La unidad de medida es obligatoria");
        }
    }

    private void validarDuplicado(String nombre, Long empresaId) {

        if (repository.existsByNombreIgnoreCaseAndEmpresaId(nombre, empresaId)) {
            throw new BusinessException("Ya existe un punto de control con ese nombre");
        }
    }

    private void validarPerteneceEmpresaActual(PuntoControl puntoControl) {
        if (!esSuperAdmin()
                && !puntoControl.getEmpresa().getId().equals(SecurityUtils.getEmpresaId())) {
            throw new BusinessException("No pertenece a la empresa");
        }
    }

    private PuntoControl construir(PuntoControlRequest request, Empresa empresa) {

        PuntoControl p = new PuntoControl();

        p.setNombre(request.getNombre());
        p.setUnidad(request.getUnidad());
        p.setValorMin(request.getValorMin());
        p.setValorMax(request.getValorMax());
        p.setEmpresa(empresa);
        p.setActivo(true);

        return p;
    }

    private PuntoControlResponse mapResponse(PuntoControl p) {
        return PuntoControlResponse.builder()
                .id(p.getId())
                .nombre(p.getNombre())
                .unidad(p.getUnidad())
                .valorMin(p.getValorMin())
                .valorMax(p.getValorMax())
                .activo(p.getActivo())
                .build();
    }

    private PuntoControl obtenerPuntoControl(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException("Punto de control no encontrado"));
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
