package cl.aracridav.svua.mantenimiento.ordenrepuesto.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.inventario.movimientoinventario.service.MovimientoInventarioService;
import cl.aracridav.svua.mantenimiento.orden.entity.EstadoOrden;
import cl.aracridav.svua.mantenimiento.orden.entity.OrdenMantenimiento;
import cl.aracridav.svua.mantenimiento.orden.repository.OrdenMantenimientoRepository;
import cl.aracridav.svua.mantenimiento.ordenrepuesto.dto.request.OrdenRepuestoRequest;
import cl.aracridav.svua.mantenimiento.ordenrepuesto.dto.response.OrdenRepuestoResponse;
import cl.aracridav.svua.mantenimiento.ordenrepuesto.entity.OrdenRepuesto;
import cl.aracridav.svua.mantenimiento.ordenrepuesto.repository.OrdenRepuestoRepository;
import cl.aracridav.svua.mantenimiento.repuesto.entity.Repuesto;
import cl.aracridav.svua.mantenimiento.repuesto.repository.RepuestoRepository;
import cl.aracridav.svua.shared.exception.BusinessException;
import cl.aracridav.svua.shared.mappers.GeneralMapper;
import cl.aracridav.svua.shared.util.SecurityUtils;
import cl.aracridav.svua.usuario.entity.Usuario;
import cl.aracridav.svua.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrdenRepuestoServiceImpl implements OrdenRepuestoService {

    private final OrdenRepuestoRepository repository;
    private final OrdenMantenimientoRepository ordenRepository;
    private final RepuestoRepository repuestoRepository;
    private final UsuarioRepository usuarioRepository;
    private final GeneralMapper mapper;
    private final MovimientoInventarioService movimientoService;

    /*
     * =========================================
     * AGREGAR REPUESTO
     * =========================================
     */
    @Override
    @Transactional
    public OrdenRepuestoResponse agregarRepuesto(OrdenRepuestoRequest request) {

        Usuario usuario = obtenerUsuarioActual();
        Empresa empresa = usuario.getEmpresa();

        OrdenMantenimiento orden = obtenerOrden(request.getOrdenId());
        Repuesto repuesto = obtenerRepuesto(request.getRepuestoId());

        validarOrden(orden);

        BigDecimal costoTotal = calcularCostoTotal(
                request.getCostoUnitario(),
                request.getCantidad()
        );

        OrdenRepuesto entity = construirOrdenRepuesto(
                request,
                orden,
                repuesto,
                usuario,
                empresa,
                costoTotal
        );

        OrdenRepuesto guardado = repository.save(entity);

        // 🔥 movimiento de inventario desacoplado
        registrarSalidaInventario(orden, repuesto, request.getCantidad());

        return mapper.mapOrdenRepuestoResponse(guardado);
    }

    /*
     * =========================================
     * LISTAR
     * =========================================
     */
    @Override
    @Transactional(readOnly = true)
    public List<OrdenRepuestoResponse> listarPorOrden(Long ordenId) {

        return repository.findByOrdenId(ordenId)
                .stream()
                .map(mapper::mapOrdenRepuestoResponse)
                .toList();
    }

    /*
     * =========================================
     * CORE
     * =========================================
     */

    private BigDecimal calcularCostoTotal(BigDecimal unitario, Integer cantidad) {
        return unitario.multiply(BigDecimal.valueOf(cantidad));
    }

    private void registrarSalidaInventario(
            OrdenMantenimiento orden,
            Repuesto repuesto,
            Integer cantidad) {

        movimientoService.salidaPorMantenimiento(
                repuesto.getId(),
                cantidad,
                "Orden mantenimiento #" + orden.getId()
        );
    }

    /*
     * =========================================
     * VALIDACIONES
     * =========================================
     */

    private void validarOrden(OrdenMantenimiento orden) {

        if (orden.getEstado() == EstadoOrden.COMPLETADA) {
            throw new BusinessException("No se pueden agregar repuestos a una orden completada");
        }
    }

    /*
     * =========================================
     * BUILDER
     * =========================================
     */

    private OrdenRepuesto construirOrdenRepuesto(
            OrdenRepuestoRequest request,
            OrdenMantenimiento orden,
            Repuesto repuesto,
            Usuario usuario,
            Empresa empresa,
            BigDecimal costoTotal) {

        OrdenRepuesto o = new OrdenRepuesto();

        o.setOrden(orden);
        o.setRepuesto(repuesto);
        o.setCantidad(request.getCantidad());
        o.setCostoUnitario(request.getCostoUnitario());
        o.setCostoTotal(costoTotal);
        o.setUsuario(usuario);
        o.setEmpresa(empresa);

        return o;
    }

    /*
     * =========================================
     * HELPERS
     * =========================================
     */

    private Usuario obtenerUsuarioActual() {
        return usuarioRepository.findById(SecurityUtils.getUsuarioId())
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
    }

    private OrdenMantenimiento obtenerOrden(Long id) {
        return ordenRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Orden no encontrada"));
    }

    private Repuesto obtenerRepuesto(Long id) {
        return repuestoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Repuesto no encontrado"));
    }
}