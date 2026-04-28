package cl.aracridav.svua.inventario.dashboard.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import cl.aracridav.svua.depreciacion.dto.DepreciacionDTO;
import cl.aracridav.svua.depreciacion.repository.DepreciacionMensualRepository;
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
    private final DepreciacionMensualRepository dMensualRepository;

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

        Pageable top6 = PageRequest.of(0, 6);

        LocalDate fechaInicio = LocalDate.now().minusMonths(5).withDayOfMonth(1);

        List<DepreciacionDTO> ultimos6meses = dMensualRepository.obtenerUltimos6Meses(empresaId, fechaInicio, top6);

        List<BigDecimal> depreciacionMensual = ultimos6meses.stream()
            .map(DepreciacionDTO::total)
            .toList();

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
                        case PROGRAMADA -> ordenesPorEstado.set(3, cantidad);
                        case CANCELADA -> ordenesPorEstado.set(4, cantidad);
                }
        }

        List<String> meses = IntStream.rangeClosed(0, 5)
            .mapToObj(i -> LocalDate.now().minusMonths(5 - i))
            .map(fecha -> fecha.getMonth()
                .getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("es-ES")))
            .map(this::capitalizar)
            .toList();

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

    private String capitalizar(String mes) {
        return mes.substring(0, 1).toUpperCase() + mes.substring(1);
    }

}
