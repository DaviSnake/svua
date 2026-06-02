package cl.aracridav.svua.notificacion.service;

import java.util.List;

import cl.aracridav.svua.mantenimiento.orden.entity.OrdenMantenimiento;
import cl.aracridav.svua.mantenimiento.repuesto.entity.Repuesto;
import cl.aracridav.svua.notificacion.dto.response.NotificacionResponse;

public interface NotificacionService {

    public void verificarStockMinimo(Repuesto repuesto);

    public void ordenPreCompletada(OrdenMantenimiento ordenMantenimiento);

    public long contarNoLeidas();

    public List<NotificacionResponse> listar();

    public void marcarComoLeida(Long id);

    public void eliminar(Long id);

}
