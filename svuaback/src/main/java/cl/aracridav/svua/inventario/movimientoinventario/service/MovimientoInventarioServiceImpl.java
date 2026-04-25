package cl.aracridav.svua.inventario.movimientoinventario.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.inventario.bodega.entity.Bodega;
import cl.aracridav.svua.inventario.bodega.repository.BodegaRepository;
import cl.aracridav.svua.inventario.movimientoinventario.dto.request.MovimientoInventarioRequest;
import cl.aracridav.svua.inventario.movimientoinventario.dto.response.MovimientoInventarioResponse;
import cl.aracridav.svua.inventario.movimientoinventario.entity.MovimientoInventario;
import cl.aracridav.svua.inventario.movimientoinventario.entity.TipoMovimiento;
import cl.aracridav.svua.inventario.movimientoinventario.repository.MovimientoInventarioRepository;
import cl.aracridav.svua.mantenimiento.repuesto.entity.Repuesto;
import cl.aracridav.svua.mantenimiento.repuesto.repository.RepuestoRepository;
import cl.aracridav.svua.mantenimiento.stock.entity.StockRepuesto;
import cl.aracridav.svua.mantenimiento.stock.repository.StockRepuestoRepository;
import cl.aracridav.svua.shared.exception.BusinessException;
import cl.aracridav.svua.shared.mappers.GeneralMapper;
import cl.aracridav.svua.shared.util.SecurityUtils;
import cl.aracridav.svua.usuario.entity.Usuario;
import cl.aracridav.svua.usuario.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MovimientoInventarioServiceImpl implements MovimientoInventarioService {

    private final MovimientoInventarioRepository repository;
    private final RepuestoRepository repuestoRepository;
    private final UsuarioRepository usuarioRepository;
    private final BodegaRepository bodegaRepository;
    private final StockRepuestoRepository stockRepository;
    private final GeneralMapper mapper;

    /*
     * =========================================
     * CREAR MOVIMIENTO
     * =========================================
     */
    @Override
    public MovimientoInventarioResponse crear(MovimientoInventarioRequest request) {

        Usuario usuario = obtenerUsuarioActual();
        Empresa empresa = usuario.getEmpresa();

        TipoMovimiento tipo = parseTipo(request.getTipo());

        StockRepuesto stock = obtenerStock(
                request.getRepuestoId(),
                request.getBodegaId(),
                empresa
        );

        int stockAnterior = stock.getCantidad();
        int stockPosterior = calcularStock(stockAnterior, request.getCantidad(), tipo);

        actualizarStock(stock, stockPosterior);

        MovimientoInventario movimiento = construirMovimiento(
                stock,
                usuario,
                empresa,
                tipo,
                request.getCantidad(),
                stockAnterior,
                stockPosterior,
                request.getReferencia(),
                request.getMotovo()
        );

        return mapper.mapMovimientoInventarioResponse(repository.save(movimiento));
    }

    /*
     * =========================================
     * LISTAR
     * =========================================
     */
    @Override
    public List<MovimientoInventarioResponse> listar() {

        Empresa empresa = obtenerUsuarioActual().getEmpresa();

        return repository.findByEmpresaId(empresa.getId())
                .stream()
                .map(mapper::mapMovimientoInventarioResponse)
                .toList();
    }

    /*
     * =========================================
     * SALIDA POR MANTENIMIENTO
     * =========================================
     */
    @Override
    @Transactional
    public void salidaPorMantenimiento(Long repuestoId, Integer cantidad, String referencia) {

        Usuario usuario = obtenerUsuarioActual();
        Empresa empresa = usuario.getEmpresa();

        StockRepuesto stock = stockRepository
                .findByRepuestoIdAndEmpresaId(repuestoId, empresa.getId())
                .orElseThrow(() -> new BusinessException("No existe stock para el repuesto"));

        int stockAnterior = stock.getCantidad();
        int stockPosterior = calcularStock(stockAnterior, cantidad, TipoMovimiento.SALIDA);

        actualizarStock(stock, stockPosterior);

        MovimientoInventario movimiento = construirMovimiento(
                stock,
                usuario,
                empresa,
                TipoMovimiento.SALIDA,
                cantidad,
                stockAnterior,
                stockPosterior,
                referencia,
                "Consumo por mantenimiento"
        );

        repository.save(movimiento);
    }

    /*
     * =========================================
     * CORE
     * =========================================
     */

    private int calcularStock(int actual, int cantidad, TipoMovimiento tipo) {

        return switch (tipo) {
            case ENTRADA -> actual + cantidad;
            case SALIDA -> {
                if (actual < cantidad) {
                    throw new BusinessException("Stock insuficiente");
                }
                yield actual - cantidad;
            }
            default -> throw new IllegalArgumentException("Unexpected value: " + tipo);
        };
    }

    private void actualizarStock(StockRepuesto stock, int nuevoStock) {
        stock.setCantidad(nuevoStock);
        stockRepository.save(stock);
    }

    /*
     * =========================================
     * BUILDER
     * =========================================
     */

    private MovimientoInventario construirMovimiento(
            StockRepuesto stock,
            Usuario usuario,
            Empresa empresa,
            TipoMovimiento tipo,
            int cantidad,
            int stockAnterior,
            int stockPosterior,
            String referencia,
            String motivo) {

        MovimientoInventario m = new MovimientoInventario();

        m.setEmpresa(empresa);
        m.setRepuesto(stock.getRepuesto());
        m.setBodega(stock.getBodega());
        m.setUsuario(usuario);

        m.setTipo(tipo);
        m.setCantidad(cantidad);
        m.setStockAnterior(stockAnterior);
        m.setStockPosterior(stockPosterior);

        m.setReferencia(referencia);
        m.setMotivo(motivo);
        m.setFecha(LocalDateTime.now());

        return m;
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

    private TipoMovimiento parseTipo(String tipo) {
        try {
            return TipoMovimiento.valueOf(tipo);
        } catch (Exception e) {
            throw new BusinessException("Tipo de movimiento inválido");
        }
    }

    private StockRepuesto obtenerStock(Long repuestoId, Long bodegaId, Empresa empresa) {

        Repuesto repuesto = repuestoRepository.findById(repuestoId)
                .orElseThrow(() -> new BusinessException("Repuesto no encontrado"));

        Bodega bodega = bodegaRepository.findById(bodegaId)
                .orElseThrow(() -> new BusinessException("Bodega no encontrada"));

        return stockRepository
                .findByRepuestoAndBodegaAndEmpresa(repuesto, bodega, empresa)
                .orElseThrow(() -> new BusinessException("Stock no encontrado"));
    }
}