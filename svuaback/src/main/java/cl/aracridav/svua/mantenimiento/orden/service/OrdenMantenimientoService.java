package cl.aracridav.svua.mantenimiento.orden.service;

import java.math.BigDecimal;
import java.util.List;

import cl.aracridav.svua.mantenimiento.orden.dto.request.OrdenMantenimientoRequest;
import cl.aracridav.svua.mantenimiento.orden.dto.response.OrdenMantenimientoResponse;
import cl.aracridav.svua.mantenimiento.orden.entity.OrdenMantenimiento;

public interface OrdenMantenimientoService {

    public OrdenMantenimientoResponse crearOrden(OrdenMantenimientoRequest request);

    public OrdenMantenimiento generarDesdePlan(Long planId, Long usuarioId);

    public OrdenMantenimiento cerrarOrden(
            Long ordenId,
            BigDecimal costo,
            String observacionesFinales
    );

    public OrdenMantenimiento cancelarOrden(Long ordenId, String motivo);

    public List<OrdenMantenimiento> obtenerOrdenesVencidas();

    public OrdenMantenimiento ejecutarOrden(Long idOrden);

    public List<OrdenMantenimientoResponse> listarOrdenesEmpresa();
    
    public OrdenMantenimientoResponse actualizarOrden(Long ordenId, OrdenMantenimientoRequest request);
}
