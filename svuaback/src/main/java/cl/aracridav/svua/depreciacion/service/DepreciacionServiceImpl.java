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
import cl.aracridav.svua.depreciacion.repository.DepreciacionMensualRepository;
import cl.aracridav.svua.depreciacion.repository.DepreciacionRepository;
import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.empresa.repository.EmpresaRepository;
import cl.aracridav.svua.inventario.activo.entity.Activo;
import cl.aracridav.svua.shared.exception.BusinessException;
import cl.aracridav.svua.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DepreciacionServiceImpl implements DepreciacionService {

    private final DepreciacionRepository depreciacionRepository;
    private final DepreciacionMensualRepository mensualRepository;
    private final EmpresaRepository empresaRepository;

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

        Depreciacion dep = new Depreciacion();
        dep.setActivo(activo);
        dep.setEmpresa(activo.getEmpresa());
        dep.setFechaInicio(activo.getFechaAdquisicion());
        dep.setMetodo(MetodoDepreciacion.LINEA_RECTA);
        dep.setValorInicial(activo.getValorAdquisicion());
        dep.setValorResidual(activo.getValorResidual());
        dep.setVidaUtilMeses(activo.getVidaUtilMeses());

        depreciacionRepository.save(dep);
    }

    @Override
    public List<DepreciacionMensual> obtenerDepreciacionesPorActivo(Activo activo) {
        return mensualRepository.findByActivoOrderByMesAsc(activo);
    }

    /*
     * =========================================
     * CORE
     * =========================================
     */

    private void guardarDepreciaciones(Activo activo, Empresa empresa) {
        List<DepreciacionMensual> lista = calcularDepreciaciones(activo, empresa);
        mensualRepository.saveAll(lista);
    }

    private List<DepreciacionMensual> calcularDepreciaciones(Activo activo, Empresa empresa) {

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
        Integer vidaUtilMeses = activo.getVidaUtilMeses();

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
                    activo, empresa, mes, fechaBase.plusMonths(mes - 1),
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

    private LocalDate obtenerFechaBase(Activo activo) {
        return activo.getFechaAdquisicion().withDayOfMonth(1);
    }

    /*
     * =========================================
     * BUILDER
     * =========================================
     */

    private DepreciacionMensual construirDepreciacionMensual(
            Activo activo,
            Empresa empresa,
            int mes,
            LocalDate fecha,
            BigDecimal depMensual,
            BigDecimal acumulada,
            BigDecimal valorContable) {

        DepreciacionMensual d = new DepreciacionMensual();

        d.setActivo(activo);
        d.setEmpresa(empresa);
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
