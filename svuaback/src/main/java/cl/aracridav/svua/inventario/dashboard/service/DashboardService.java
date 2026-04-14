package cl.aracridav.svua.inventario.dashboard.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import cl.aracridav.svua.depreciacion.repository.DepreciacionRepository;
import cl.aracridav.svua.inventario.activo.repository.ActivoRepository;
import cl.aracridav.svua.inventario.dashboard.dto.response.DashboardResponse;
import cl.aracridav.svua.mantenimiento.orden.entity.EstadoOrden;
import cl.aracridav.svua.mantenimiento.orden.repository.OrdenMantenimientoRepository;
import cl.aracridav.svua.shared.enums.EstadoActivo;
import cl.aracridav.svua.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ActivoRepository activoRepository;
    private final OrdenMantenimientoRepository ordenRepository;
    private final DepreciacionRepository depreciacionRepository;

    public DashboardResponse obtenerDashboard() {

        Long empresaId = SecurityUtils.getEmpresaId();

        // KPIs
        Long totalActivos = activoRepository.countByEmpresaId(empresaId);

        Long activosOperativos =
                activoRepository.countByEmpresaIdAndEstadoActual(empresaId, EstadoActivo.OPERATIVO);

        Long activosFueraServicio =
                activoRepository.countByEmpresaIdAndEstadoActual(empresaId, EstadoActivo.FUERA_SERVICIO);

        BigDecimal valorTotal =
                activoRepository.sumValorByEmpresa(empresaId);

        BigDecimal depreciacion =
                depreciacionRepository.depreciacionTotal(empresaId);

        Long ordenesAbiertas =
                ordenRepository.countByEmpresaIdAndEstadoIn(
                        empresaId,
                        List.of(EstadoOrden.PENDIENTE, EstadoOrden.EN_EJECUCION)
                );

        Long vencidos =
                ordenRepository.countMantenimientosVencidos(empresaId);

        // 📊 ORDENES POR ESTADO
        List<Object[]> estados = ordenRepository.countOrdenesPorEstado(empresaId);

        List<Long> ordenesPorEstado = new ArrayList<>(List.of(0L, 0L, 0L, 0L, 0L));

        for (Object[] row : estados) {
                EstadoOrden estado = (EstadoOrden) row[0];
                Long cantidad = ((Number) row[1]).longValue();

                switch (estado) {
                        case PENDIENTE -> ordenesPorEstado.set(0, cantidad);
                        case EN_EJECUCION -> ordenesPorEstado.set(1, cantidad);
                        case COMPLETADA -> ordenesPorEstado.set(2, cantidad);
                        case CANCELADA -> ordenesPorEstado.set(3, cantidad);
                        case PROGRAMADA -> ordenesPorEstado.set(4, cantidad);
                }
        }

        // 📉 DEPRECIACIÓN MOCK (puedes mejorar después)
        List<String> meses = List.of("Ene", "Feb", "Mar", "Abr", "May", "Jun");
        List<BigDecimal> depreciacionMensual = List.of(
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(12000),
                BigDecimal.valueOf(15000),
                BigDecimal.valueOf(17000),
                BigDecimal.valueOf(20000),
                BigDecimal.valueOf(22000)
        );

        return DashboardResponse.builder()
                .totalActivos(totalActivos)
                .activosOperativos(activosOperativos)
                .activosFueraServicio(activosFueraServicio)
                .valorTotal(valorTotal)
                .depreciacionAcumulada(depreciacion)
                .ordenesAbiertas(ordenesAbiertas)
                .mantenimientosVencidos(vencidos)
                .ordenesPorEstado(ordenesPorEstado)
                .meses(meses)
                .depreciacionMensual(depreciacionMensual)
                .build();
    }

}
