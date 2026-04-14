package cl.aracridav.svua.shared.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;

import cl.aracridav.svua.inventario.activo.entity.Activo;
import cl.aracridav.svua.shared.dto.response.VidaUtilResponse;
import cl.aracridav.svua.shared.enums.EstadoVidaUtil;
import cl.aracridav.svua.shared.exception.BusinessException;

@Service
public class VidaUtilService {

    public VidaUtilResponse calcularVidaUtil(Activo activo) {

        if (activo.getFechaAdquisicion() == null) {
            throw new BusinessException("El activo no tiene fecha de adquisición");
        }

        if (activo.getVidaUtilMeses() == null || activo.getVidaUtilMeses() <= 0) {
            throw new BusinessException("Vida útil inválida para el activo");
        }

        LocalDate fechaInicio = activo.getFechaAdquisicion();
        LocalDate fechaFin = fechaInicio.plusMonths(activo.getVidaUtilMeses());
        LocalDate hoy = LocalDate.now();

        long mesesConsumidos = ChronoUnit.MONTHS.between(fechaInicio, hoy);
        mesesConsumidos = Math.max(mesesConsumidos, 0);

        long mesesRestantes = ChronoUnit.MONTHS.between(hoy, fechaFin);
        mesesRestantes = Math.max(mesesRestantes, 0);

        double porcentajeConsumido =
                Math.min(100.0,
                        (double) mesesConsumidos / activo.getVidaUtilMeses() * 100);

        EstadoVidaUtil estado;

        if (hoy.isAfter(fechaFin)) {
            estado = EstadoVidaUtil.VENCIDO;
        } else if (mesesRestantes <= 3) {
            estado = EstadoVidaUtil.POR_VENCER;
        } else {
            estado = EstadoVidaUtil.VIGENTE;
        }

        return VidaUtilResponse.builder()
                .fechaInicio(fechaInicio)
                .fechaFin(fechaFin)
                .mesesTotales(activo.getVidaUtilMeses())
                .mesesConsumidos((int) mesesConsumidos)
                .mesesRestantes((int) mesesRestantes)
                .porcentajeConsumido(porcentajeConsumido)
                .estado(estado)
                .build();
    }

}
