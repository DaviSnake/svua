package cl.aracridav.svua.inventario.activo.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.aracridav.svua.inventario.activo.dto.request.ActivoCreateRequest;
import cl.aracridav.svua.inventario.activo.dto.request.ActivoUpdateRequest;
import cl.aracridav.svua.inventario.activo.dto.request.DarDeBajaActivoRequest;
import cl.aracridav.svua.inventario.activo.dto.response.ActivoResponse;
import cl.aracridav.svua.inventario.activo.service.ActivoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(value = "/api/v1/svua/activos")
@RequiredArgsConstructor
public class ActivoController {

    private final ActivoService activoService;

    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA') or " +
        "(hasAuthority('ACTIVO_CREATE')) "
    )
    @PostMapping
    public ResponseEntity<ActivoResponse> registrarActivo(
            @RequestBody ActivoCreateRequest request) {

        ActivoResponse response = activoService.crearActivo(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA') or " +
        "(hasAuthority('ACTIVO_CREATE')) "
    )
    @PutMapping("/{id}")
    public ResponseEntity<ActivoResponse> actualizarActivo(
        @PathVariable Long id,
        @RequestBody ActivoUpdateRequest request) {

        ActivoResponse response = activoService.actualizarActivo(id, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // ===============================
    // DAR DE BAJA
    // ===============================
    @PatchMapping("/{id}/baja")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA')  or " +
                "(hasAuthority('ACTIVO_CREATE'))")
    public ResponseEntity<Void> darDeBaja(
            @PathVariable Long id,
            @Valid @RequestBody DarDeBajaActivoRequest request) {

        activoService.darDeBaja(id, request);
        return ResponseEntity.noContent().build();
    }
    
    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA') or " +
        "(hasAuthority('ACTIVO_VIEW')) "
    )
    @GetMapping
    public ResponseEntity<Page<ActivoResponse>> mostrarActivos(Pageable pegable) {

        Page<ActivoResponse> response = activoService.mostrarActivos(pegable);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{id}/riesgo")
    public Map<String, Object> getRiesgo(@PathVariable Long id) {

        double riesgo = activoService.calcularRiesgo(id);

        Map<String, Object> response = new HashMap<>();
        response.put("riesgo", riesgo);
        response.put("nivel", activoService.nivelRiesgo(riesgo));

        return response;
    }

}
