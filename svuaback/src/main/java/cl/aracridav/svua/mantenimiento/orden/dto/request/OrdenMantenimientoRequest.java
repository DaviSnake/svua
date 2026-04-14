package cl.aracridav.svua.mantenimiento.orden.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import cl.aracridav.svua.mantenimiento.orden.entity.EstadoOrden;
import cl.aracridav.svua.mantenimiento.plan.entity.TipoMantenimiento;
import lombok.Data;

@Data
public class OrdenMantenimientoRequest {

    private String titulo;
    private LocalDateTime fechaProgramada;
    private TipoMantenimiento tipoMantenimiento;
    private EstadoOrden estado;
    private BigDecimal costo;
    private String observaciones;
    private Long activoId;
    private Long usuarioId;
    private Long planMantenimientoId;

}
