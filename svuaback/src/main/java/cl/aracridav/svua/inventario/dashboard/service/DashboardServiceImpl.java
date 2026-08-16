package cl.aracridav.svua.inventario.dashboard.service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import cl.aracridav.svua.inventario.dashboard.dto.response.DashboardIndicadoresResponse;
import cl.aracridav.svua.inventario.dashboard.dto.response.DashboardResponse;
import cl.aracridav.svua.inventario.dashboard.dto.response.IndicadorCumplimientoResponse;
import cl.aracridav.svua.inventario.dashboard.dto.response.IndicadorMTTRResponse;
import cl.aracridav.svua.mantenimiento.orden.entity.EstadoOrden;
import cl.aracridav.svua.mantenimiento.orden.entity.OrdenMantenimiento;
import cl.aracridav.svua.mantenimiento.orden.entity.TipoMantenimiento;
import cl.aracridav.svua.mantenimiento.orden.repository.OrdenMantenimientoRepository;
import cl.aracridav.svua.shared.enums.EstadoActivo;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final ActivoRepository activoRepository;
    private final OrdenMantenimientoRepository ordenRepository;
    private final DepreciacionRepository depreciacionRepository;
    private final DepreciacionMensualRepository dMensualRepository;

    @Override
    public DashboardResponse obtenerDashboardFull(Long empresaId) {

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

        List<Long> ordenesPorEstado = new ArrayList<>(List.of(0L, 0L, 0L, 0L, 0L, 0L, 0L));

        for (Object[] row : estados) {
            EstadoOrden estado = (EstadoOrden) row[0];
            Long cantidad = ((Number) row[1]).longValue();

            switch (estado) {
                case PENDIENTE -> ordenesPorEstado.set(0, cantidad);
                case EN_EJECUCION -> ordenesPorEstado.set(1, cantidad);
                case PRE_COMPLETADA -> ordenesPorEstado.set(2, cantidad);
                case COMPLETADA -> ordenesPorEstado.set(3, cantidad);
                case PROGRAMADA -> ordenesPorEstado.set(4, cantidad);
                case CANCELADA -> ordenesPorEstado.set(5, cantidad);
                case ATRASADA -> ordenesPorEstado.set(6, cantidad);
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

    @Override
    public DashboardIndicadoresResponse obtenerDashboard(Long empresaId) {

        long programadas = ordenRepository.countByEmpresaId(empresaId);

        long completadas = ordenRepository.countByEmpresaIdAndEstado(
                empresaId, EstadoOrden.COMPLETADA);

        long pendientes = ordenRepository.countByEmpresaIdAndEstado(
                empresaId, EstadoOrden.PENDIENTE);

        long atrasadas = ordenRepository.countByEmpresaIdAndEstado(
                empresaId, EstadoOrden.ATRASADA);

        long canceladas = ordenRepository.countByEmpresaIdAndEstado(
                empresaId, EstadoOrden.CANCELADA);

        double cumplimiento =
                programadas == 0 ? 0 :
                ((double) completadas / programadas) * 100;

        // MTTR
        Double mttrSeg =
                ordenRepository.avgDuracionByEmpresa(empresaId);

        double mttr = mttrSeg == null ? 0 : mttrSeg / 3600.0;

        // MTBF (simplificado)
        double mtbf = calcularMTBF(empresaId);

        return DashboardIndicadoresResponse.builder()
                .programadas(programadas)
                .completadas(completadas)
                .pendientes(pendientes)
                .atrasadas(atrasadas)
                .canceladas(canceladas)
                .cumplimiento(Math.round(cumplimiento * 100.0) / 100.0)
                .mttrHoras(Math.round(mttr * 100.0) / 100.0)
                .mtbfHoras(mtbf)
                .build();
    }
    
    @Override
    public double calcularCumplimiento(Long empresaId) {

        long programadas =
                ordenRepository.countByEmpresaId(empresaId);

        long completadas =
                ordenRepository.countByEmpresaIdAndEstado(
                        empresaId,
                        EstadoOrden.COMPLETADA);

        if (programadas == 0) {
            return 0;
        }

        double cumplimiento =
                ((double) completadas / programadas) * 100;

        return Math.round(cumplimiento * 100.0) / 100.0;
    }
    
    @Override
    public IndicadorCumplimientoResponse obtenerIndicadorSemanal(Long empresaId) {
        return calcularIndicadorCumplimiento(empresaId, inicioSemanaActual(), finSemanaActual());
    }

    @Override
    public IndicadorCumplimientoResponse obtenerIndicadorMensual(Long empresaId) {
        return calcularIndicadorCumplimiento(empresaId, inicioMesActual(), finMesActual());
    }

    private IndicadorCumplimientoResponse calcularIndicadorCumplimiento(
            Long empresaId, LocalDateTime inicio, LocalDateTime fin) {

        long programadas =
            ordenRepository.contarProgramadas(
                empresaId,
                EstadoOrden.CANCELADA,
                inicio,
                fin);

        long completadas =
            ordenRepository.contarCompletadas(
                empresaId,
                EstadoOrden.COMPLETADA,
                inicio,
                fin);

        double cumplimiento = 0;

        if (programadas > 0) {

            cumplimiento =
                    ((double) completadas
                    / programadas) * 100;

            cumplimiento =
                    Math.round(cumplimiento * 100.0)
                    / 100.0;
        }

        return IndicadorCumplimientoResponse
                .builder()
                .programadas(programadas)
                .completadas(completadas)
                .cumplimiento(cumplimiento)
                .build();
    }
    
    @Override
    public IndicadorMTTRResponse obtenerMTTRMensual(Long empresaId) {

        LocalDateTime inicio = inicioMesActual();

        LocalDateTime fin = finMesActual();

        Double promedioSegundos =
            ordenRepository.calcularMTTR(
                empresaId,
                EstadoOrden.COMPLETADA,
                TipoMantenimiento.CORRECTIVO,
                inicio,
                fin);

        long cantidad =
            ordenRepository.contarOrdenesMTTR(
                empresaId,
                EstadoOrden.COMPLETADA,
                TipoMantenimiento.CORRECTIVO,
                inicio,
                fin);

        if (promedioSegundos == null) {
            promedioSegundos = 0.0;
        }

        double mttrHoras =
                promedioSegundos / 3600.0;

        mttrHoras =
                Math.round(mttrHoras * 100.0)
                / 100.0;

        return IndicadorMTTRResponse
                .builder()
                .mttrHoras(mttrHoras)
                .ordenesConsideradas(cantidad)
                .build();
    }

    private String capitalizar(String mes) {
        return mes.substring(0, 1).toUpperCase() + mes.substring(1);
    }

    private LocalDateTime inicioSemanaActual() {

        return LocalDate.now()
                .with(DayOfWeek.MONDAY)
                .atStartOfDay();
    }
    
    private LocalDateTime finSemanaActual() {

        return LocalDate.now()
            .with(DayOfWeek.SUNDAY)
            .atTime(23,59,59);
    }

    private LocalDateTime inicioMesActual() {

        return LocalDate.now()
                .withDayOfMonth(1)
                .atStartOfDay();
    }

    private LocalDateTime finMesActual() {

        return LocalDate.now()
                .withDayOfMonth(
                        LocalDate.now().lengthOfMonth())
                .atTime(23,59,59);
    }

    private double calcularMTBF(Long empresaId) {

        List<OrdenMantenimiento> fallas =
                ordenRepository.findFallasMTBF(empresaId);

        if (fallas == null || fallas.size() < 2) {
            return 0;
        }

        long totalSegundos = 0;
        int intervalos = 0;

        for (int i = 1; i < fallas.size(); i++) {

            LocalDateTime finAnterior =
                    fallas.get(i - 1).getFechaFinEjecucion();

            LocalDateTime inicioActual =
                    fallas.get(i).getFechaEjecucion();

            if (finAnterior == null || inicioActual == null) {
                continue;
            }

            long diff = Duration.between(
                    finAnterior,
                    inicioActual
            ).getSeconds();

            totalSegundos += diff;
            intervalos++;
        }

        if (intervalos == 0) {
            return 0;
        }

        double mtbfHoras =
                (totalSegundos / 3600.0) / intervalos;

        return Math.round(mtbfHoras * 100.0) / 100.0;
    }
}
