package cl.aracridav.svua.inventario.movimientoinventario.service;

import java.util.List;

import cl.aracridav.svua.inventario.movimientoinventario.dto.request.MovimientoInventarioRequest;
import cl.aracridav.svua.inventario.movimientoinventario.dto.response.MovimientoInventarioResponse;

public interface MovimientoInventarioService {

    public MovimientoInventarioResponse crear(MovimientoInventarioRequest request);

    public List<MovimientoInventarioResponse> listar();

    public void salidaPorMantenimiento(Long repuestoId, Integer cantidad, String referencia);

}
