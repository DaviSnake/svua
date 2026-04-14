package cl.aracridav.svua.mantenimiento.plan.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.aracridav.svua.mantenimiento.plan.dto.request.PlanMantenimientoCreateRequest;
import cl.aracridav.svua.mantenimiento.plan.dto.response.PlanMantenimientoReponse;
import cl.aracridav.svua.mantenimiento.plan.service.PlanMantenimientoService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/svua/planes-mantenimiento")
@RequiredArgsConstructor
public class PlanMantenimientoController {

    private final PlanMantenimientoService service;

    @PreAuthorize(
        "hasRole('SUPER_ADMIN') or " +
        "(hasAuthority('PLAN_CREATE')) "
    )
    @PostMapping
    public ResponseEntity<PlanMantenimientoReponse> crear(
            @RequestBody PlanMantenimientoCreateRequest dto
    ) {
        return ResponseEntity.ok(service.crear(dto));
    }

    @PreAuthorize(
        "hasRole('SUPER_ADMIN') or " +
        "(hasAuthority('PLAN_VIEW')) "
    )
    @GetMapping
    public ResponseEntity<List<PlanMantenimientoReponse>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @PreAuthorize(
        "hasRole('SUPER_ADMIN') or " +
        "(hasAuthority('PLAN_VIEW')) "
    )
    @GetMapping("/{id}")
    public ResponseEntity<PlanMantenimientoReponse> obtener(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(service.obtener(id));
    }

    @PreAuthorize(
        "hasRole('SUPER_ADMIN') or " +
        "(hasAuthority('PLAN_DELETE')) "
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id
    ) {
        service.desactivar(id);
        return ResponseEntity.noContent().build();
    }

}
