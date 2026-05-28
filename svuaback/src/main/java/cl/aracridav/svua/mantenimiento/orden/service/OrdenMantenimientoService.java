package cl.aracridav.svua.mantenimiento.orden.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import cl.aracridav.svua.mantenimiento.orden.dto.request.ActualizarOrdenMantenimientoRequest;
import cl.aracridav.svua.mantenimiento.orden.dto.request.OrdenMantenimientoRequest;
import cl.aracridav.svua.mantenimiento.orden.dto.response.CostosGraficoReponse;
import cl.aracridav.svua.mantenimiento.orden.dto.response.OrdenEjecucionResponse;
import cl.aracridav.svua.mantenimiento.orden.dto.response.OrdenMantenimientoResponse;
import cl.aracridav.svua.mantenimiento.orden.entity.OrdenMantenimiento;

public interface OrdenMantenimientoService {

    public OrdenMantenimientoResponse crearOrden(OrdenMantenimientoRequest request);

    public OrdenMantenimiento generarDesdePlan(Long planId, Long usuarioId);

    public OrdenEjecucionResponse cerrarOrden(
            Long ordenId,
            BigDecimal costo,
            String observacionesFinales
    );

    public OrdenEjecucionResponse detenerOrden(Long idOrden);

    public OrdenEjecucionResponse detenerOrden(Long idOrden, MultipartFile archivo);

    public void cancelarOrden(Long id, String motivo, Long usuarioId);

    public List<OrdenMantenimiento> obtenerOrdenesVencidas();

    public OrdenEjecucionResponse ejecutarOrden(Long idOrden);

    public List<OrdenMantenimientoResponse> listarOrdenesEmpresa();
    
    public OrdenMantenimientoResponse reprogramarOrden(Long ordenId, LocalDateTime nuevaFecha, String motivo);

    public OrdenMantenimientoResponse actualizar(
            Long id,
            ActualizarOrdenMantenimientoRequest request);

    public CostosGraficoReponse obtenerGraficoCostosUltimos6Meses();
}
