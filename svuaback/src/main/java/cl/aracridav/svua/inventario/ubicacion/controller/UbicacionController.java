package cl.aracridav.svua.inventario.ubicacion.controller;

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

import cl.aracridav.svua.inventario.ubicacion.dto.request.UbicacionCreateRequest;
import cl.aracridav.svua.inventario.ubicacion.dto.response.UbicacionResponse;
import cl.aracridav.svua.inventario.ubicacion.service.UbicacionService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(value = "/api/v1/svua/ubicaciones")
@RequiredArgsConstructor
public class UbicacionController {

    private final UbicacionService ubicacionService;

    @PreAuthorize(
        "hasRole('SUPER_ADMIN') or " +
        "(hasAuthority('UBICACION_CREATE')) "
    )
    @PostMapping
    public ResponseEntity<UbicacionResponse> registrarUbicacion(
            @RequestBody UbicacionCreateRequest request) {

        UbicacionResponse response = ubicacionService.registrarUbicacion(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PreAuthorize(
        "hasRole('SUPER_ADMIN') or " +
        "(hasAuthority('UBICACION_VIEW')) "
    )
    @GetMapping
    public ResponseEntity<Page<UbicacionResponse>> listarUbicaciones(Pageable pegable) {

        Page<UbicacionResponse> response = ubicacionService.listarUbicaciones(pegable);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

}
