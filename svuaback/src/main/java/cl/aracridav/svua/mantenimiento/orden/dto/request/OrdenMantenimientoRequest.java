package cl.aracridav.svua.mantenimiento.orden.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import cl.aracridav.svua.mantenimiento.orden.entity.EstadoOrden;
import cl.aracridav.svua.mantenimiento.orden.entity.TipoMantenimiento;
import cl.aracridav.svua.mantenimiento.ordenrepuesto.dto.request.OrdenRepuestoRequest;
import lombok.Data;

@Data
public class OrdenMantenimientoRequest {

    private String titulo;
    private LocalDateTime fechaProgramada;
    private Long duracionMinutos;
    private TipoMantenimiento tipoMantenimiento;
    private EstadoOrden estado;
    private BigDecimal costoTotal;
    private BigDecimal horasEstimadas;
    private BigDecimal valorHora;
    private BigDecimal costoManoObraEstimada;
    private BigDecimal costoManoObra;
    private String observaciones;
    private Long activoId;
    private Long usuarioId;
    private Long proveedorId;
    private List<OrdenRepuestoRequest> repuestos;

    // 🔥 Ingreso retroactivo (solo SUPER_ADMIN / ADMIN_EMPRESA, ver
    // OrdenMantenimientoServiceImpl.construirOrdenRetroactiva): permite
    // declarar una orden ya completada, con la fecha/hora real de
    // inicio y termino del trabajo (hasta 24 horas atras), en vez de
    // pasar por el flujo en vivo (ejecutar -> pre-detener -> completar).
    private Boolean ingresoRetroactivo;
    private LocalDateTime fechaEjecucionReal;
    private LocalDateTime fechaFinEjecucionReal;

}
