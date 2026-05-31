package cl.aracridav.svua.notificacion.service;

import cl.aracridav.svua.mantenimiento.repuesto.entity.Repuesto;

public interface NotificacionService {

    public void verificarStockMinimo(Repuesto repuesto);

}
