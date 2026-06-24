package cl.aracridav.svua.mantenimiento.orden.dto.request;

import java.util.List;

import cl.aracridav.svua.mantenimiento.orden.entity.TipoMantenimiento;
import cl.aracridav.svua.mantenimiento.ordenrepuesto.dto.request.OrdenRepuestoRequest;
import lombok.Data;

@Data
public class ActualizarOrdenMantenimientoRequest {

    private String titulo;
    private Long duracionMinutos;
    private TipoMantenimiento tipoMantenimiento;
    private String observaciones;
    private List<OrdenRepuestoRequest> repuestos;

}
