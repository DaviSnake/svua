package cl.aracridav.svua.depreciacion.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.aracridav.svua.depreciacion.entity.DepreciacionMensual;
import cl.aracridav.svua.depreciacion.entity.TipoDepreciacion;
import cl.aracridav.svua.depreciacion.service.DepreciacionService;
import cl.aracridav.svua.inventario.activo.entity.Activo;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(value = "/api/v1/svua/depreciacion")
@RequiredArgsConstructor
public class DepreciacionController {

    private final DepreciacionService depreciacionService;

    // Calcular y guardar depreciaciones
    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA') or " +
        "(hasAuthority('DEPRECIACION_CREATE')) "
    )
    @PostMapping("/mensual/guardar")
    public void calcularYGuardar(@RequestBody Activo activo) {
        depreciacionService.calcularYGuardarDepreciacionMensual(activo);
    }

    // Consultar depreciaciones ya calculadas de un activo. `tipo` elige
    // entre el cronograma NORMAL (contable, default) y el ACELERADA
    // (tributario, SII).
    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA') or " +
        "(hasAuthority('DEPRECIACION_VIEW')) "
    )
    @GetMapping("/mensual/{activoId}")
    public List<DepreciacionMensual> obtenerDepreciaciones(
            @PathVariable Long activoId,
            @RequestParam(defaultValue = "NORMAL") TipoDepreciacion tipo) {
        Activo activo = new Activo();
        activo.setId(activoId);
        return depreciacionService.obtenerDepreciacionesPorActivo(activo, tipo);
    }

    // Backfill: genera la depreciación ACELERADA para los activos de la
    // empresa actual que aún no la tienen. Se puede correr varias veces
    // sin duplicar (solo procesa los que faltan). Devuelve cuántos
    // activos fueron procesados.
    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA') or " +
        "(hasAuthority('DEPRECIACION_CREATE')) "
    )
    @PostMapping("/acelerada/generar-faltantes")
    public int generarDepreciacionAceleradaFaltante() {
        return depreciacionService.generarDepreciacionAceleradaFaltante();
    }

    // Genera la depreciación ACELERADA de un activo puntual. Falla si
    // ya la tenía calculada (evita duplicarla).
    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA') or " +
        "(hasAuthority('DEPRECIACION_CREATE')) "
    )
    @PostMapping("/acelerada/generar/{activoId}")
    public void generarDepreciacionAceleradaPorActivo(@PathVariable Long activoId) {
        depreciacionService.generarDepreciacionAceleradaPorActivo(activoId);
    }

}
