package cl.aracridav.svua.mantenimiento.orden.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import cl.aracridav.svua.mantenimiento.orden.entity.EstadoOrden;
import cl.aracridav.svua.mantenimiento.orden.entity.TipoMantenimiento;
import cl.aracridav.svua.mantenimiento.ordenrepuesto.dto.response.OrdenRepuestoResponse;
import lombok.Data;

@Data
public class OrdenMantenimientoResponse {

    private Long id;
    private String titulo;
    private LocalDateTime fechaProgramada;
    private LocalDateTime fechaTermino;
    private Long duracionMinutos;
    private LocalDateTime fechaEjecucion;
    private TipoMantenimiento tipoMantenimiento;
    private EstadoOrden estado;
    private BigDecimal costoTotal;
    private BigDecimal horasEstimadas;
    private BigDecimal horasReal;
    private BigDecimal valorHora;
    private BigDecimal costoManoObraEstimada;
    private BigDecimal costoManoObra;
    private String observaciones;
    private Long activoId;
    private Long usuarioId;
    private Long proveedorId;
    private List<OrdenRepuestoResponse> repuestos;

    // 🔥 le permite al frontend saber si ya se adjuntó el checklist
    // (sin exponer la ruta real del archivo en el servidor).
    private boolean tieneChecklist;

}
