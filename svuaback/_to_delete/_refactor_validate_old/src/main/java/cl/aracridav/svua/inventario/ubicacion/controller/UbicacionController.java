package cl.aracridav.svua.inventario.ubicacion.controller;

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

import cl.aracridav.svua.inventario.ubicacion.dto.request.UbicacionCreateRequest;
import cl.aracridav.svua.inventario.ubicacion.dto.response.UbicacionResponse;
import cl.aracridav.svua.inventario.ubicacion.service.UbicacionService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(value = "/api/v1/svua/ubicaciones")
@RequiredArgsConstructor
public class UbicacionController {

    private final UbicacionService ubicacionService;

    /*
     * =========================================
     * CREAR
     * =========================================
     */
    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA') or hasAuthority('UBICACION_CREATE')"
    )
    @PostMapping
    public ResponseEntity<UbicacionResponse> registrarUbicacion(
            @RequestBody UbicacionCreateRequest request) {

        UbicacionResponse response =
                ubicacionService.registrarUbicacion(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /*
     * =========================================
     * LISTAR
     * =========================================
     */
    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA') or hasAuthority('UBICACION_VIEW')"
    )
    @GetMapping
    public ResponseEntity<Page<UbicacionResponse>> listarUbicaciones(
            Pageable pageable) {

        Page<UbicacionResponse> response =
                ubicacionService.listarUbicaciones(pageable);

        return ResponseEntity.ok(response); // ✅ FIX
    }

    /*
     * =========================================
     * OBTENER
     * =========================================
     */
    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA') or hasAuthority('UBICACION_VIEW')"
    )
    @GetMapping("/{id}")
    public ResponseEntity<UbicacionResponse> obtener(
            @PathVariable Long id) {

        UbicacionResponse response =
                ubicacionService.obtener(id);

        return ResponseEntity.ok(response);
    }

    /*
     * =========================================
     * ACTUALIZAR
     * =========================================
     */
    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA') or hasAuthority('UBICACION_UPDATE')"
    )
    @PutMapping("/{id}")
    public ResponseEntity<UbicacionResponse> actualizar(
            @PathVariable Long id,
            @RequestBody UbicacionCreateRequest request) {

        UbicacionResponse response =
                ubicacionService.actualizar(id, request);

        return ResponseEntity.ok(response);
    }

    /*
     * =========================================
     * ELIMINAR (SOFT DELETE)
     * =========================================
     */
    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA') or hasAuthority('UBICACION_DELETE')"
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {

        ubicacionService.eliminar(id);

        return ResponseEntity.noContent().build(); // ✅ 204 correcto
    }
}