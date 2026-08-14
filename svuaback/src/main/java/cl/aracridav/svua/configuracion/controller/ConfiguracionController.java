package cl.aracridav.svua.configuracion.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.aracridav.svua.configuracion.dto.request.ActualizarConfiguracionRequest;
import cl.aracridav.svua.configuracion.dto.response.ConfiguracionEntryResponse;
import cl.aracridav.svua.configuracion.service.ConfiguracionService;
import lombok.RequiredArgsConstructor;

// 🔒 Configuración global de la infraestructura (.env real usado por
// docker-compose): incluye credenciales sensibles (BD, JWT, correo, API
// de pagos), por eso el acceso es exclusivo de SUPER_ADMIN.
@RestController
@RequestMapping("/api/v1/svua/configuracion")
@RequiredArgsConstructor
public class ConfiguracionController {

    private final ConfiguracionService configuracionService;

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping
    public ResponseEntity<List<ConfiguracionEntryResponse>> obtenerConfiguracion() {
        return ResponseEntity.ok(configuracionService.leerConfiguracion());
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PutMapping
    public ResponseEntity<Void> actualizarConfiguracion(
            @RequestBody ActualizarConfiguracionRequest request) {

        configuracionService.actualizarConfiguracion(request.getValores());

        return ResponseEntity.ok().build();
    }

}
