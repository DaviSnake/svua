package cl.aracridav.svua.inventario.bodega.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.aracridav.svua.inventario.bodega.dto.request.BodegaRequest;
import cl.aracridav.svua.inventario.bodega.dto.response.BodegaResponse;
import cl.aracridav.svua.inventario.bodega.service.BodegaService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/svua/bodegas")
@RequiredArgsConstructor
public class BodegaController {

    private final BodegaService service;

    @PreAuthorize(
        "hasRole('SUPER_ADMIN') or " +
        "(hasAuthority('BODEGA_CREATE')) "
    )
    @PostMapping
    public ResponseEntity<BodegaResponse> crear(@RequestBody BodegaRequest request) {

        return ResponseEntity.ok(service.crear(request));
    }

    @PreAuthorize(
        "hasRole('SUPER_ADMIN') or " +
        "(hasAuthority('BODEGA_VIEW')) "
    )
    @GetMapping
    public ResponseEntity<Page<BodegaResponse>> listar(Pageable pegable) {

        return ResponseEntity.ok(service.listar(pegable));
    }

    @PreAuthorize(
        "hasRole('SUPER_ADMIN') or " +
        "(hasAuthority('BODEGA_VIEW')) "
    )
    @GetMapping("/{id}")
    public ResponseEntity<BodegaResponse> obtener(@PathVariable Long id) {

        return ResponseEntity.ok(service.obtener(id));
    }

    @PreAuthorize(
        "hasRole('SUPER_ADMIN') or " +
        "(hasAuthority('BODEGA_UPDATE')) "
    )
    @PutMapping("/{id}")
    public ResponseEntity<BodegaResponse> actualizar(
            @PathVariable Long id,
            @RequestBody BodegaRequest request) {

        return ResponseEntity.ok(service.actualizar(id, request));
    }

    @PreAuthorize(
        "hasRole('SUPER_ADMIN') or " +
        "(hasAuthority('BODEGA_DELETE')) "
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {

        service.eliminar(id);

        return ResponseEntity.noContent().build();
    }

}
