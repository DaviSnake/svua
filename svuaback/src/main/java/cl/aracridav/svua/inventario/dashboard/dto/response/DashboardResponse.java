package cl.aracridav.svua.inventario.dashboard.dto.response;

import java.math.BigDecimal;
import java.util.List;

import lombok.*;

@Data
@Builder
public class DashboardResponse {

    private Long totalActivos;
    private Long activosOperativos;
    private Long activosFueraServicio;

    private BigDecimal valorTotal;
    private BigDecimal depreciacionAcumulada;

    private Long ordenesAbiertas;
    private Long mantenimientosVencidos;

    // PRO
    private List<BigDecimal> depreciacionMensual;
    private List<String> meses;
    private List<Long> ordenesPorEstado;

}
