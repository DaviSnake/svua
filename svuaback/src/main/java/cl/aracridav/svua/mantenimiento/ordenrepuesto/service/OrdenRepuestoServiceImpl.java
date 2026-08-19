package cl.aracridav.svua.mantenimiento.ordenrepuesto.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.notificacion.service.NotificacionService;
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
    private final NotificacionService notificacionService;

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

        // 🔒 el stock real que usa toda la app es Repuesto.stockActual —
        // el mismo campo que se edita en el maestro de repuestos. Antes
        // acá se descontaba en StockRepuesto (stock por bodega, vía
        // movimientos de inventario), un sistema paralelo que para estos
        // repuestos nunca se cargó, y que no tiene relación con lo que
        // el usuario ve/edita en el maestro — de ahí el error "No existe
        // stock para el repuesto".
        if (repuesto.getStockActual() < request.getCantidad()) {
            throw new BusinessException("Stock insuficiente para: " + repuesto.getNombre());
        }

        repuesto.setStockActual(repuesto.getStockActual() - request.getCantidad());

        Repuesto repuestoGuardado = repuestoRepository.save(repuesto);

        notificacionService.verificarStockMinimo(repuestoGuardado);

        // 🔥 si el repuesto YA estaba agregado a esta orden, se SUMA la
        // cantidad en la misma fila en vez de crear una fila duplicada.
        Optional<OrdenRepuesto> existenteOpt =
                repository.findByOrdenIdAndRepuestoId(orden.getId(), repuesto.getId());

        OrdenRepuesto guardado;

        if (existenteOpt.isPresent()) {
            OrdenRepuesto existente = existenteOpt.get();

            int nuevaCantidad = existente.getCantidad() + request.getCantidad();

            existente.setCantidad(nuevaCantidad);
            existente.setCostoUnitario(request.getCostoUnitario());
            existente.setCostoTotal(
                    request.getCostoUnitario().multiply(BigDecimal.valueOf(nuevaCantidad))
            );

            guardado = repository.save(existente);
        } else {

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

            guardado = repository.save(entity);
        }

        actualizarCostoTotalOrden(orden);

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
     * ELIMINAR
     * =========================================
     */
    @Override
    @Transactional
    public void eliminarRepuesto(Long id) {

        OrdenRepuesto item = repository.findById(id)
                .orElseThrow(() -> new BusinessException("Repuesto de la orden no encontrado"));

        OrdenMantenimiento orden = item.getOrden();

        // 🔒 misma regla que al agregar: no se toca una orden ya completada.
        if (orden.getEstado() == EstadoOrden.COMPLETADA) {
            throw new BusinessException("No se pueden eliminar repuestos de una orden completada");
        }

        repository.delete(item);

        // 🔥 repone el stock (mismo campo Repuesto.stockActual que se
        // descontó al agregar, no el sistema paralelo de StockRepuesto).
        Repuesto repuesto = item.getRepuesto();
        repuesto.setStockActual(repuesto.getStockActual() + item.getCantidad());
        repuestoRepository.save(repuesto);

        actualizarCostoTotalOrden(orden);
    }

    /*
     * =========================================
     * CORE
     * =========================================
     */

    private BigDecimal calcularCostoTotal(BigDecimal unitario, Integer cantidad) {
        return unitario.multiply(BigDecimal.valueOf(cantidad));
    }

    // 🔥 recalcula el costoTotal de la orden desde cero (mano de obra +
    // suma real de todos sus repuestos en BD), en vez de ir sumando/
    // restando incrementalmente — así no importa el orden de flush entre
    // el save/delete recién hecho y esta consulta, y no se acumula
    // ningún desvío si algo quedó desincronizado antes.
    private void actualizarCostoTotalOrden(OrdenMantenimiento orden) {

        BigDecimal costoManoObra = Optional.ofNullable(orden.getCostoManoObraProveedor())
                .orElse(BigDecimal.ZERO);

        BigDecimal costoRepuestos = Optional.ofNullable(repository.calcularCostoOrden(orden.getId()))
                .orElse(BigDecimal.ZERO);

        orden.setCostoTotal(costoManoObra.add(costoRepuestos));

        ordenRepository.save(orden);
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