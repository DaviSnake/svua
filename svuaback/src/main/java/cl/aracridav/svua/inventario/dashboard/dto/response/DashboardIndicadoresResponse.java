package cl.aracridav.svua.inventario.dashboard.dto.response;

import java.util.List;
import java.util.Map;

import lombok.*;

@Getter
@Setter
@Builder
public class DashboardIndicadoresResponse {

    private long programadas;
    private long preCompletadas;
    private long completadas;
    private long pendientes;
    private long atrasadas;
    private long canceladas;

    private double cumplimientoPreventivo;
    private double cumplimientoCorrectivo;

    private double disponibilidad;

    private double mttrHoras;
    private double mtbfHoras;

    private Map<String, Long> porEstado;

    private List<String> meses;
    private List<Double> cumplimientoMensual;
}
