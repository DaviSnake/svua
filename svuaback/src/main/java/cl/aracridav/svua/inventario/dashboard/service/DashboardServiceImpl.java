package cl.aracridav.svua.inventario.dashboard.service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
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

        Long activosDeBaja =
            activoRepository.countByEmpresaIdAndEstadoActual(empresaId, EstadoActivo.BAJA);

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
                .activosDeBaja(activosDeBaja)
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

        // "Total Ordenes" y el denominador de Cumplimiento excluyen
        // ambos las canceladas: una orden cancelada no es parte del
        // universo de ordenes de trabajo real, asi que no debe afectar
        // (ni sumar ni restar) ninguno de los dos calculos.
        long totalOrdenes = ordenRepository.countByEmpresaIdAndEstadoNot(
                empresaId, EstadoOrden.CANCELADA);

        long preCompletadas = ordenRepository.countByEmpresaIdAndEstado(
                empresaId, EstadoOrden.PRE_COMPLETADA);

        long completadas = ordenRepository.countByEmpresaIdAndEstado(
                empresaId, EstadoOrden.COMPLETADA);

        long pendientes = ordenRepository.countByEmpresaIdAndEstado(
                empresaId, EstadoOrden.PENDIENTE);

        long atrasadas = ordenRepository.countByEmpresaIdAndEstado(
                empresaId, EstadoOrden.ATRASADA);

        long canceladas = ordenRepository.countByEmpresaIdAndEstado(
                empresaId, EstadoOrden.CANCELADA);

        // 🔥 Cumplimiento separado por tipo de mantenimiento: mismo
        // calculo de antes (completadas / total, sin contar canceladas),
        // pero acotado a PREVENTIVO y a CORRECTIVO por separado. Las
        // ordenes PREDICTIVO no entran en ninguna de las dos tarjetas.
        long totalPreventivas = ordenRepository.countByEmpresaIdAndEstadoNotAndTipoMantenimiento(
                empresaId, EstadoOrden.CANCELADA, TipoMantenimiento.PREVENTIVO);

        long completadasPreventivas = ordenRepository.countByEmpresaIdAndEstadoAndTipoMantenimiento(
                empresaId, EstadoOrden.COMPLETADA, TipoMantenimiento.PREVENTIVO);

        double cumplimientoPreventivo =
                totalPreventivas == 0 ? 0 :
                ((double) completadasPreventivas / totalPreventivas) * 100;

        long totalCorrectivas = ordenRepository.countByEmpresaIdAndEstadoNotAndTipoMantenimiento(
                empresaId, EstadoOrden.CANCELADA, TipoMantenimiento.CORRECTIVO);

        long completadasCorrectivas = ordenRepository.countByEmpresaIdAndEstadoAndTipoMantenimiento(
                empresaId, EstadoOrden.COMPLETADA, TipoMantenimiento.CORRECTIVO);

        double cumplimientoCorrectivo =
                totalCorrectivas == 0 ? 0 :
                ((double) completadasCorrectivas / totalCorrectivas) * 100;

        // Disponibilidad = (Horas programadas - Horas de detencion) /
        // Horas programadas x 100 (ver calcularDisponibilidad).
        double disponibilidad = calcularDisponibilidad(empresaId);

        // MTTR
        Double mttrSeg =
                ordenRepository.avgDuracionByEmpresa(empresaId);

        double mttr = mttrSeg == null ? 0 : mttrSeg / 3600.0;

        // MTBF: promedio del MTBF de cada activo (ver calcularMTBF)
        double mtbf = calcularMTBF(empresaId);

        return DashboardIndicadoresResponse.builder()
                .programadas(totalOrdenes)
                .preCompletadas(preCompletadas)
                .completadas(completadas)
                .pendientes(pendientes)
                .atrasadas(atrasadas)
                .canceladas(canceladas)
                .cumplimientoPreventivo(Math.round(cumplimientoPreventivo * 100.0) / 100.0)
                .cumplimientoCorrectivo(Math.round(cumplimientoCorrectivo * 100.0) / 100.0)
                .disponibilidad(disponibilidad)
                .mttrHoras(Math.round(mttr * 100.0) / 100.0)
                .mtbfHoras(mtbf)
                .build();
    }

    // 🔧 Disponibilidad = (Horas programadas - Horas de detencion) /
    // Horas programadas x 100.
    //
    // "Horas programadas" = horas de calendario transcurridas en lo que
    // va del mes actual, multiplicadas por la cantidad de activos de la
    // empresa (cada activo "deberia" estar disponible todo ese tiempo).
    // "Horas de detencion" = suma real de duracionSegundos de las
    // ordenes COMPLETADA cuya fechaFinEjecucion cae en ese mismo tramo.
    //
    // 🔒 Antes se comparaba contra la duracion ESTIMADA del ticket
    // (fechaTermino - fechaProgramada, un numero chico ingresado al
    // crear la orden) en vez de contra horas de calendario: como la
    // detencion real casi siempre supera esa estimacion (la orden queda
    // EN_EJECUCION de un dia para otro, fin de semana, etc.), el
    // resultado daba negativo. Con el periodo de calendario como base,
    // el denominador es siempre mayor o igual a la detencion real salvo
    // que la empresa realmente haya estado mas tiempo detenida que
    // operativa.
    private double calcularDisponibilidad(Long empresaId) {

        LocalDateTime inicio = inicioMesActual();
        LocalDateTime fin = LocalDateTime.now();

        long totalActivos = activoRepository.countByEmpresaId(empresaId);

        if (totalActivos == 0) {
            return 0;
        }

        double horasPeriodo = Duration.between(inicio, fin).getSeconds() / 3600.0;

        if (horasPeriodo <= 0) {
            return 0;
        }

        double horasProgramadas = horasPeriodo * totalActivos;

        Long segundosDetencion =
            ordenRepository.sumDuracionSegundosCompletadasEnPeriodo(empresaId, inicio, fin);

        double horasDetencion = (segundosDetencion != null ? segundosDetencion : 0L) / 3600.0;

        double disponibilidad = ((horasProgramadas - horasDetencion) / horasProgramadas) * 100;

        return Math.round(disponibilidad * 100.0) / 100.0;
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

    // 🔧 MTBF calculado POR ACTIVO y luego promediado entre activos.
    // Antes se mezclaban las fallas de TODOS los activos de la empresa
    // en una sola linea de tiempo ordenada por fecha: eso no solo media
    // algo distinto a un MTBF real ("cada cuanto falla algo en toda la
    // flota" en vez de "cada cuanto falla un equipo"), sino que ademas
    // podia dar intervalos NEGATIVOS si dos activos distintos tenian
    // fallas superpuestas en el tiempo (el fin de la reparacion de uno
    // quedaba despues del inicio de la falla de otro). Al agrupar por
    // activo, las ordenes de un mismo equipo no se solapan entre si, asi
    // que el problema desaparece de raiz.
    private double calcularMTBF(Long empresaId) {

        List<OrdenMantenimiento> fallas =
                ordenRepository.findFallasMTBF(empresaId);

        if (fallas == null || fallas.isEmpty()) {
            return 0;
        }

        Map<Long, List<OrdenMantenimiento>> fallasPorActivo = fallas.stream()
                .collect(Collectors.groupingBy(o -> o.getActivo().getId()));

        List<Double> mtbfPorActivo = new ArrayList<>();

        for (List<OrdenMantenimiento> fallasActivo : fallasPorActivo.values()) {

            if (fallasActivo.size() < 2) {
                continue;
            }

            // 🔥 La query ya trae todo ordenado por activo + fecha, pero
            // se re-ordena por las dudas al agrupar (el orden relativo
            // entre grupos no garantiza el orden dentro de cada uno).
            fallasActivo.sort(Comparator.comparing(OrdenMantenimiento::getFechaEjecucion));

            long totalSegundos = 0;
            int intervalos = 0;

            for (int i = 1; i < fallasActivo.size(); i++) {

                LocalDateTime finAnterior =
                        fallasActivo.get(i - 1).getFechaFinEjecucion();

                LocalDateTime inicioActual =
                        fallasActivo.get(i).getFechaEjecucion();

                if (finAnterior == null || inicioActual == null) {
                    continue;
                }

                long diff = Duration.between(
                        finAnterior,
                        inicioActual
                ).getSeconds();

                // 🔒 Defensivo: dentro de un mismo activo esto ya no
                // deberia pasar, pero si algun dato quedara inconsistente
                // se descarta el intervalo en vez de sumarlo negativo.
                if (diff < 0) {
                    continue;
                }

                totalSegundos += diff;
                intervalos++;
            }

            if (intervalos > 0) {
                mtbfPorActivo.add((totalSegundos / 3600.0) / intervalos);
            }
        }

        if (mtbfPorActivo.isEmpty()) {
            return 0;
        }

        double promedio = mtbfPorActivo.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0);

        return Math.round(promedio * 100.0) / 100.0;
    }
}
