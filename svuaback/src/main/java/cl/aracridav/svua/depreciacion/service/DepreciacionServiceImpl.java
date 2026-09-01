package cl.aracridav.svua.depreciacion.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.aracridav.svua.depreciacion.entity.Depreciacion;
import cl.aracridav.svua.depreciacion.entity.DepreciacionMensual;
import cl.aracridav.svua.depreciacion.entity.MetodoDepreciacion;
import cl.aracridav.svua.depreciacion.entity.TipoDepreciacion;
import cl.aracridav.svua.depreciacion.repository.DepreciacionMensualRepository;
import cl.aracridav.svua.depreciacion.repository.DepreciacionRepository;
import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.empresa.repository.EmpresaRepository;
import cl.aracridav.svua.inventario.activo.entity.Activo;
import cl.aracridav.svua.inventario.activo.repository.ActivoRepository;
import cl.aracridav.svua.shared.exception.BusinessException;
import cl.aracridav.svua.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DepreciacionServiceImpl implements DepreciacionService {

    // 🔒 SII (Art. 31 N°5 LIR): la vida útil acelerada, para efectos
    // tributarios, no puede ser inferior a 1 año — si el activo ya
    // tiene una vida útil normal de 12 meses o menos, no hay nada que
    // acelerar (usar la misma vida útil).
    private static final int VIDA_UTIL_ACELERADA_MINIMA_MESES = 12;

    private final DepreciacionRepository depreciacionRepository;
    private final DepreciacionMensualRepository mensualRepository;
    private final EmpresaRepository empresaRepository;
    private final ActivoRepository activoRepository;

    /*
     * =========================================
     * PUBLIC API
     * =========================================
     */

    @Override
    public void calcularYGuardarDepreciacionMensual(Activo activo) {
        Empresa empresa = obtenerEmpresaActual();
        guardarDepreciaciones(activo, empresa);
    }

    @Override
    public void calcularYGuardarDepreciacionMensual(Activo activo, Long empresaId) {
        Empresa empresa = obtenerEmpresa(empresaId);
        guardarDepreciaciones(activo, empresa);
    }

    @Override
    public void guardarDepreciacion(Activo activo) {

        Integer vidaNormal = activo.getVidaUtilMeses();
        Integer vidaAcelerada = calcularVidaUtilAcelerada(vidaNormal);

        depreciacionRepository.save(
                construirDepreciacionHeader(activo, TipoDepreciacion.NORMAL, vidaNormal));

        depreciacionRepository.save(
                construirDepreciacionHeader(activo, TipoDepreciacion.ACELERADA, vidaAcelerada));
    }

    @Override
    public List<DepreciacionMensual> obtenerDepreciacionesPorActivo(Activo activo) {
        return obtenerDepreciacionesPorActivo(activo, TipoDepreciacion.NORMAL);
    }

    @Override
    public List<DepreciacionMensual> obtenerDepreciacionesPorActivo(Activo activo, TipoDepreciacion tipo) {
        return mensualRepository.findByActivoAndTipoOrderByMesAsc(activo, tipo);
    }

    @Override
    public int generarDepreciacionAceleradaFaltante() {

        Empresa empresa = obtenerEmpresaActual();

        List<Activo> activos = activoRepository.findActivosSinDepreciacionAcelerada(empresa.getId());

        for (Activo activo : activos) {
            generarDepreciacionAcelerada(activo, empresa);
        }

        return activos.size();
    }

    @Override
    public void generarDepreciacionAceleradaPorActivo(Long activoId) {

        Activo activo = activoRepository.findById(activoId)
                .orElseThrow(() -> new BusinessException("Activo no encontrado"));

        // 🔒 Idempotencia: sin esto, llamar dos veces al mismo activo
        // duplicaría su cabecera y su cronograma ACELERADA.
        if (mensualRepository.existsByActivoIdAndTipo(activoId, TipoDepreciacion.ACELERADA)) {
            throw new BusinessException("El activo ya tiene depreciación acelerada calculada");
        }

        generarDepreciacionAcelerada(activo, activo.getEmpresa());
    }

    /*
     * =========================================
     * CORE
     * =========================================
     */

    private void guardarDepreciaciones(Activo activo, Empresa empresa) {

        Integer vidaNormal = activo.getVidaUtilMeses();
        Integer vidaAcelerada = calcularVidaUtilAcelerada(vidaNormal);

        List<DepreciacionMensual> lista = new ArrayList<>();
        lista.addAll(calcularDepreciaciones(activo, empresa, vidaNormal, TipoDepreciacion.NORMAL));
        lista.addAll(calcularDepreciaciones(activo, empresa, vidaAcelerada, TipoDepreciacion.ACELERADA));

        mensualRepository.saveAll(lista);
    }

    // 🔁 Compartido por el alta de un activo nuevo (guardarDepreciacion)
    // y por el backfill de activos existentes (generarDepreciacion*):
    // arma la cabecera + el cronograma mensual ACELERADA y los guarda.
    private void generarDepreciacionAcelerada(Activo activo, Empresa empresa) {

        Integer vidaAcelerada = calcularVidaUtilAcelerada(activo.getVidaUtilMeses());

        depreciacionRepository.save(
                construirDepreciacionHeader(activo, TipoDepreciacion.ACELERADA, vidaAcelerada));

        List<DepreciacionMensual> lista =
                calcularDepreciaciones(activo, empresa, vidaAcelerada, TipoDepreciacion.ACELERADA);

        mensualRepository.saveAll(lista);
    }

    private List<DepreciacionMensual> calcularDepreciaciones(
            Activo activo, Empresa empresa, Integer vidaUtilMesesInput, TipoDepreciacion tipo) {

        List<DepreciacionMensual> lista = new ArrayList<>();

        BigDecimal costo = activo.getValorAdquisicion();
        BigDecimal residual = activo.getValorResidual();

        // 🔒 `vidaUtilMeses` es el denominador del cálculo de depreciación
        // mensual. Ya se valida al crear/actualizar el activo
        // (ActivoServiceImpl.validarVidaUtilMeses), pero se vuelve a
        // validar acá como segunda barrera: sin esto, un activo con vida
        // útil en null o 0 (por ejemplo, uno cargado antes de que existiera
        // esa validación, o vía otro flujo que llame directamente a este
        // método) rompía el cálculo con un NullPointerException o una
        // ArithmeticException ("/ by zero").
        Integer vidaUtilMeses = vidaUtilMesesInput;

        if (vidaUtilMeses == null || vidaUtilMeses <= 0) {
            vidaUtilMeses = 1;
        }

        int vida = vidaUtilMeses;

        BigDecimal totalDepreciable = costo.subtract(residual);
        BigDecimal depMensual = calcularDepreciacionMensual(totalDepreciable, vida);

        BigDecimal acumulada = BigDecimal.ZERO;

        LocalDate fechaBase = obtenerFechaBase(activo);

        for (int mes = 1; mes <= vida; mes++) {

            // 🔒 El último mes no usa la cuota fija: cierra con lo que
            // falte por depreciar (totalDepreciable - acumulada previa). Sin
            // esto, el redondeo HALF_UP de la cuota mensual (aplicado `vida`
            // veces) dejaba activos que nunca llegaban exacto a su valor
            // residual (si el redondeo fue hacia abajo), o un
            // depreciacionAcumulada que superaba el total depreciable (si
            // fue hacia arriba), inconsistente con el valor contable.
            BigDecimal cuota = (mes == vida)
                    ? totalDepreciable.subtract(acumulada)
                    : depMensual;

            acumulada = acumulada.add(cuota);
            BigDecimal valorContable = costo.subtract(acumulada);

            lista.add(construirDepreciacionMensual(
                    activo, empresa, tipo, mes, fechaBase.plusMonths(mes - 1),
                    cuota, acumulada, valorContable
            ));
        }

        return lista;
    }

    /*
     * =========================================
     * HELPERS CÁLCULO
     * =========================================
     */

    private BigDecimal calcularDepreciacionMensual(BigDecimal totalDepreciable, int vida) {
        return totalDepreciable.divide(BigDecimal.valueOf(vida), RoundingMode.HALF_UP);
    }

    // SII (Art. 31 N°5 LIR): 1/3 de la vida útil normal, con un piso de
    // 1 año. Si la vida útil normal ya es de 1 año o menos, no hay
    // margen para acelerar: se usa la misma vida útil normal.
    private Integer calcularVidaUtilAcelerada(Integer vidaUtilMesesNormal) {

        if (vidaUtilMesesNormal == null || vidaUtilMesesNormal <= 0) {
            return vidaUtilMesesNormal;
        }

        if (vidaUtilMesesNormal <= VIDA_UTIL_ACELERADA_MINIMA_MESES) {
            return vidaUtilMesesNormal;
        }

        int tercio = (int) Math.round(vidaUtilMesesNormal / 3.0);

        return Math.max(VIDA_UTIL_ACELERADA_MINIMA_MESES, tercio);
    }

    private LocalDate obtenerFechaBase(Activo activo) {
        return activo.getFechaAdquisicion().withDayOfMonth(1);
    }

    /*
     * =========================================
     * BUILDER
     * =========================================
     */

    private Depreciacion construirDepreciacionHeader(Activo activo, TipoDepreciacion tipo, Integer vidaUtilMeses) {

        Depreciacion dep = new Depreciacion();
        dep.setActivo(activo);
        dep.setEmpresa(activo.getEmpresa());
        dep.setFechaInicio(activo.getFechaAdquisicion());
        dep.setMetodo(MetodoDepreciacion.LINEA_RECTA);
        dep.setTipo(tipo);
        dep.setValorInicial(activo.getValorAdquisicion());
        dep.setValorResidual(activo.getValorResidual());
        dep.setVidaUtilMeses(vidaUtilMeses);

        return dep;
    }

    private DepreciacionMensual construirDepreciacionMensual(
            Activo activo,
            Empresa empresa,
            TipoDepreciacion tipo,
            int mes,
            LocalDate fecha,
            BigDecimal depMensual,
            BigDecimal acumulada,
            BigDecimal valorContable) {

        DepreciacionMensual d = new DepreciacionMensual();

        d.setActivo(activo);
        d.setEmpresa(empresa);
        d.setTipo(tipo);
        d.setMes(mes);
        d.setFecha(fecha);
        d.setDepreciacionMensual(depMensual);
        d.setDepreciacionAcumulada(acumulada);
        d.setValorContable(valorContable);

        return d;
    }

    /*
     * =========================================
     * HELPERS
     * =========================================
     */

    private Empresa obtenerEmpresaActual() {
        return obtenerEmpresa(SecurityUtils.getEmpresaId());
    }

    private Empresa obtenerEmpresa(Long id) {
        return empresaRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Empresa no encontrada"));
    }
}
