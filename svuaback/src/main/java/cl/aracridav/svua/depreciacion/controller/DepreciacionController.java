package cl.aracridav.svua.depreciacion.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.aracridav.svua.depreciacion.entity.DepreciacionMensual;
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
        "hasRole('SUPER_ADMIN') or " +
        "(hasAuthority('DEPRECIACION_CREATE')) "
    )
    @PostMapping("/mensual/guardar")
    public void calcularYGuardar(@RequestBody Activo activo) {
        depreciacionService.calcularYGuardarDepreciacionMensual(activo);
    }

    // Consultar depreciaciones ya calculadas de un activo
    @PreAuthorize(
        "hasRole('SUPER_ADMIN') or " +
        "(hasAuthority('DEPRECIACION_VIEW')) "
    )
    @GetMapping("/mensual/{activoId}")
    public List<DepreciacionMensual> obtenerDepreciaciones(@PathVariable Long activoId) {
        Activo activo = new Activo();
        activo.setId(activoId);
        return depreciacionService.obtenerDepreciacionesPorActivo(activo);
    }

}
