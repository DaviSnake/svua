package cl.aracridav.svua.mantenimiento.ordenrepuesto.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.aracridav.svua.mantenimiento.ordenrepuesto.dto.request.OrdenRepuestoRequest;
import cl.aracridav.svua.mantenimiento.ordenrepuesto.dto.response.OrdenRepuestoResponse;
import cl.aracridav.svua.mantenimiento.ordenrepuesto.service.OrdenRepuestoService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/svua/orden-repuestos")
@RequiredArgsConstructor
public class OrdenRepuestoController {

    private final OrdenRepuestoService service;

    @PreAuthorize(
        "hasRole('SUPER_ADMIN') or " +
        "(hasAuthority('ORDEN_REPUESTO_CREATE')) "
    )
    @PostMapping
    public ResponseEntity<OrdenRepuestoResponse> agregarRepuesto(
            @RequestBody OrdenRepuestoRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.agregarRepuesto(request));
    }

    @PreAuthorize(
        "hasRole('SUPER_ADMIN') or " +
        "(hasAuthority('ORDEN_REPUESTO_VIEW')) "
    )
    @GetMapping("/orden/{ordenId}")
    public ResponseEntity<List<OrdenRepuestoResponse>> listarPorOrden(
            @PathVariable Long ordenId) {

        return ResponseEntity.ok(service.listarPorOrden(ordenId));
    }

}
