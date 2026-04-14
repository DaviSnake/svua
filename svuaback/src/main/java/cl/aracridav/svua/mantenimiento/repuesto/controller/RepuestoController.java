package cl.aracridav.svua.mantenimiento.repuesto.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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

import cl.aracridav.svua.mantenimiento.repuesto.dto.request.RepuestoRequest;
import cl.aracridav.svua.mantenimiento.repuesto.dto.response.RepuestoResponse;
import cl.aracridav.svua.mantenimiento.repuesto.service.RepuestoService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/svua/repuestos")
@RequiredArgsConstructor
public class RepuestoController {

    private final RepuestoService repuestoService;

    @PreAuthorize(
        "hasRole('SUPER_ADMIN') or " +
        "(hasAuthority('REPUESTO_CREATE')) "
    )
    @PostMapping
    public RepuestoResponse crear(@RequestBody RepuestoRequest request) {
        return repuestoService.crear(request);
    }

    @PreAuthorize(
        "hasRole('SUPER_ADMIN') or " +
        "(hasAuthority('REPUESTO_VIEW')) "
    )
    @GetMapping
    public ResponseEntity<Page<RepuestoResponse>> listarRepuestos(Pageable pegable) {

        Page<RepuestoResponse> response = repuestoService.listarRepuestos(pegable);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PreAuthorize(
        "hasRole('SUPER_ADMIN') or " +
        "(hasAuthority('REPUESTO_VIEW')) "
    )
    @GetMapping("/{id}")
    public RepuestoResponse obtener(@PathVariable Long id) {
        return repuestoService.obtener(id);
    }

    @PreAuthorize(
        "hasRole('SUPER_ADMIN') or " +
        "(hasAuthority('REPUESTO_UPDATE')) "
    )
    @PutMapping("/{id}")
    public RepuestoResponse actualizar(
            @PathVariable Long id,
            @RequestBody RepuestoRequest request) {

        return repuestoService.actualizar(id, request);
    }

    @PreAuthorize(
        "hasRole('SUPER_ADMIN') or " +
        "(hasAuthority('REPUESTO_DELETE')) "
    )
    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        repuestoService.eliminar(id);
    }

}
