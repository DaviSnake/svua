package cl.aracridav.svua.inventario.activo.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.aracridav.svua.inventario.activo.dto.request.ActivoCreateRequest;
import cl.aracridav.svua.inventario.activo.dto.response.ActivoResponse;
import cl.aracridav.svua.inventario.activo.service.ActivoService;
import cl.aracridav.svua.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(value = "/api/v1/svua/activos")
@RequiredArgsConstructor
public class ActivoController {

    private final ActivoService activoService;

    @PreAuthorize(
        "hasRole('SUPER_ADMIN') or " +
        "(hasAuthority('ACTIVO_CREATE')) "
    )
    @PostMapping
    public ResponseEntity<ActivoResponse> registrarActivo(
            @RequestBody ActivoCreateRequest request) {

        Long empresaId = SecurityUtils.getEmpresaId();

        ActivoResponse response = activoService.crearActivo(empresaId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
    
    @PreAuthorize(
        "hasRole('SUPER_ADMIN') or " +
        "(hasAuthority('ACTIVO_VIEW')) "
    )
    @GetMapping
    public ResponseEntity<Page<ActivoResponse>> mostrarActivos(Pageable pegable) {

        Page<ActivoResponse> response = activoService.mostrarActivos(pegable);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

}
