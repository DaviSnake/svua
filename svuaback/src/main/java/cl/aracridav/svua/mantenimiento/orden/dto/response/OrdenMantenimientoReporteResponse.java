package cl.aracridav.svua.mantenimiento.orden.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import cl.aracridav.svua.mantenimiento.orden.entity.EstadoOrden;
import cl.aracridav.svua.mantenimiento.orden.entity.TipoMantenimiento;
import cl.aracridav.svua.mantenimiento.ordenrepuesto.dto.response.OrdenRepuestoResponse;
import lombok.Builder;
import lombok.Data;

// 🔥 Informe de Mantenciones: comprobante por orden de mantención
// completada, visible solo para SUPER_ADMIN. A diferencia de
// OrdenMantenimientoResponse (que solo trae IDs), este DTO resuelve los
// nombres (activo, empresa, usuario, proveedor) para mostrarlos
// directamente en el informe sin llamadas adicionales.
@Data
@Builder
public class OrdenMantenimientoReporteResponse {

    private Long id;
    private String titulo;
    private String observaciones;
    private EstadoOrden estado;
    private TipoMantenimiento tipoMantenimiento;
    private LocalDateTime fechaProgramada;
    private LocalDateTime fechaEjecucion;

    private String activoNombre;
    private String empresaNombre;
    private String usuarioNombre;
    private String proveedorNombre;

    private BigDecimal valorHoraProveedor;
    private BigDecimal costoManoObraProveedor;
    private BigDecimal costoTotal;

    private List<OrdenRepuestoResponse> repuestos;

}
