package cl.aracridav.svua.inventario.movimientoinventario.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.empresa.repository.EmpresaRepository;
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
    private final StockRepuestoRepository stockRepuestoRepository;
    private final EmpresaRepository empresaRepository;
    private final GeneralMapper generalMapper;

    public MovimientoInventarioResponse crear(MovimientoInventarioRequest request) {

        Long usuarioId = SecurityUtils.getUsuarioId();

        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new BusinessException("Usuario no encontrado"));

        Empresa empresa = empresaRepository.findById(usuario.getEmpresa().getId())
            .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

        Repuesto repuesto = repuestoRepository.findById(request.getRepuestoId())
                .orElseThrow(() -> new BusinessException("Repuesto no encontrado"));

         Bodega bodega = bodegaRepository.findById(request.getBodegaId())
            .orElseThrow(() -> new BusinessException("Bodega no encontrada"));

        StockRepuesto stock = stockRepuestoRepository
            .findByRepuestoAndBodegaAndEmpresa(repuesto, bodega, empresa)
            .orElseThrow(() -> new BusinessException("Stock no encontrado"));

        //Calculo de los stock
        Integer stockAnterior = stock.getCantidad();
        Integer stockPosterior;

        if (request.getTipo() == TipoMovimiento.ENTRADA.toString()) {

            stockPosterior = stockAnterior + request.getCantidad();

        } else if (request.getTipo() == TipoMovimiento.SALIDA.toString()) {

            if (stockAnterior < request.getCantidad()) {
                throw new BusinessException("Stock insuficiente");
            }

            stockPosterior = stockAnterior - request.getCantidad();

        } else {
            throw new BusinessException("Tipo de movimiento inválido");
        }

        // actualizar stock
        stock.setCantidad(stockPosterior);
        stockRepuestoRepository.save(stock);

        MovimientoInventario movimiento = new MovimientoInventario();

        movimiento.setEmpresa(usuario.getEmpresa());
        movimiento.setRepuesto(repuesto);
        movimiento.setTipo(TipoMovimiento.valueOf(request.getTipo()));
        movimiento.setCantidad(request.getCantidad());
        movimiento.setStockAnterior(stockAnterior);
        movimiento.setStockPosterior(stockPosterior);
        movimiento.setReferencia(request.getReferencia());
        movimiento.setMotivo(request.getMotovo());
        movimiento.setFecha(LocalDateTime.now());
        movimiento.setBodega(bodega);
        movimiento.setUsuario(usuario);

        MovimientoInventario mInventario = repository.save(movimiento);

        return generalMapper.mapMovimientoInventarioResponse(mInventario);
    }

    public List<MovimientoInventarioResponse> listar() {

        Long usuarioId = SecurityUtils.getUsuarioId();

        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new BusinessException("Usuario no encontrado"));

        return repository.findByEmpresaId(usuario.getEmpresa().getId())
                .stream()
                .map(generalMapper::mapMovimientoInventarioResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void salidaPorMantenimiento(Long repuestoId, Integer cantidad, String referencia) {

        Long usuarioId = SecurityUtils.getUsuarioId();

        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new BusinessException("Usuario no encontrado"));

        Empresa empresa = empresaRepository.findById(usuario.getEmpresa().getId())
            .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

        // Buscar stock del repuesto
        StockRepuesto stock = stockRepuestoRepository
                .findByRepuestoIdAndEmpresaId(repuestoId, empresa.getId())
                .orElseThrow(() -> 
                    new BusinessException("No existe stock para el repuesto"));

        Integer stockAnterior = stock.getCantidad();

        // Validar stock
        if (stockAnterior < cantidad) {
            throw new BusinessException("Stock insuficiente para el repuesto");
        }

        // Calcular nuevo stock
        Integer stockPosterior = stockAnterior - cantidad;

        // Actualizar stock
        stock.setCantidad(stockPosterior);
        stockRepuestoRepository.save(stock);

        // Crear movimiento inventario
        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setRepuesto(stock.getRepuesto());
        movimiento.setTipo(TipoMovimiento.SALIDA);
        movimiento.setCantidad(cantidad);
        movimiento.setStockAnterior(stockAnterior);
        movimiento.setStockPosterior(stockPosterior);
        movimiento.setFecha(LocalDateTime.now());
        movimiento.setReferencia(referencia);
        movimiento.setMotivo("Consumo por mantenimiento");
        movimiento.setUsuario(usuario);
        movimiento.setBodega(stock.getBodega());
        movimiento.setEmpresa(empresa);

        repository.save(movimiento);
    }
}
