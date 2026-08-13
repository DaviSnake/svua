package cl.aracridav.svua.inventario.dashboard.service;

import cl.aracridav.svua.inventario.dashboard.dto.response.DashboardIndicadoresResponse;
import cl.aracridav.svua.inventario.dashboard.dto.response.DashboardResponse;
import cl.aracridav.svua.inventario.dashboard.dto.response.IndicadorCumplimientoResponse;
import cl.aracridav.svua.inventario.dashboard.dto.response.IndicadorMTTRResponse;

public interface DashboardService {

    public DashboardResponse obtenerDashboardFull(Long empresaId);

    public double calcularCumplimiento(Long empresaId);

    public IndicadorCumplimientoResponse obtenerIndicadorSemanal(Long empresaId);
    
    public IndicadorCumplimientoResponse obtenerIndicadorMensual(Long empresaId);

    public IndicadorMTTRResponse obtenerMTTRMensual(Long empresaId);

    public DashboardIndicadoresResponse obtenerDashboard(Long empresaId);

}
