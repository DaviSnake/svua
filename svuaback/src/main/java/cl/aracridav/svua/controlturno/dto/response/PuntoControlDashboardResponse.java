package cl.aracridav.svua.controlturno.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Data;

// 🔥 Datos pre-agregados para el dashboard de graficos de un
// PuntoControl (ver LecturaControlServiceImpl.dashboard): el frontend
// solo dibuja, no recalcula nada.
@Data
@Builder
public class PuntoControlDashboardResponse {

    private Long puntoControlId;
    private String nombre;
    private String unidad;
    private BigDecimal valorMin;
    private BigDecimal valorMax;

    // 🔥 Para el grafico de linea: evolucion de las lecturas en el
    // rango de fechas filtrado (mismo indice = mismo punto).
    private List<LocalDateTime> fechas;
    private List<BigDecimal> valores;

    // 🔥 Para el grafico de dona: cantidad de lecturas dentro vs fuera
    // del rango aceptable (valorMin/valorMax). Si el punto de control
    // no tiene rango definido, ambos quedan en 0 y el frontend debe
    // omitir la dona para ese punto.
    private long lecturasDentroRango;
    private long lecturasFueraRango;
}
