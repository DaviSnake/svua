package cl.aracridav.svua.inventario.bodega.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.aracridav.svua.inventario.bodega.dto.request.BodegaRequest;
import cl.aracridav.svua.inventario.bodega.dto.response.BodegaResponse;
import cl.aracridav.svua.inventario.bodega.service.BodegaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/svua/bodegas")
@RequiredArgsConstructor
public class BodegaController {

    private final BodegaService bodegaService;

    /*
     * =========================================
     * CREAR
     * =========================================
     */
    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA') or hasAuthority('BODEGA_CREATE')"
    )
    @PostMapping
    public ResponseEntity<BodegaResponse> crear(
            @Valid @RequestBody BodegaRequest request) {

        BodegaResponse response = bodegaService.crear(request);

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
        "hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA') or hasAuthority('BODEGA_VIEW')"
    )
    @GetMapping
    public ResponseEntity<Page<BodegaResponse>> listar(
            Pageable pageable,
            @RequestParam(required = false) Long empresaId,
            @RequestParam(required = false) String busqueda) {

        // 🔥 empresaId es opcional: permite filtrar la grilla por empresa
        // (solo tiene efecto para SUPER_ADMIN, ver BodegaServiceImpl).
        // 🔥 busqueda es opcional: filtra por nombre.
        Page<BodegaResponse> response = bodegaService.listar(pageable, empresaId, busqueda);

        return ResponseEntity.ok(response);
    }

    /*
     * =========================================
     * OBTENER
     * =========================================
     */
    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA') or hasAuthority('BODEGA_VIEW')"
    )
    @GetMapping("/{id}")
    public ResponseEntity<BodegaResponse> obtener(@PathVariable Long id) {

        BodegaResponse response = bodegaService.obtener(id);

        return ResponseEntity.ok(response);
    }

    /*
     * =========================================
     * ACTUALIZAR
     * =========================================
     */
    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA') or hasAuthority('BODEGA_UPDATE')"
    )
    @PutMapping("/{id}")
    public ResponseEntity<BodegaResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody BodegaRequest request) {

        BodegaResponse response = bodegaService.actualizar(id, request);

        return ResponseEntity.ok(response);
    }

    /*
     * =========================================
     * ELIMINAR (SOFT DELETE)
     * =========================================
     */
    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA') or hasAuthority('BODEGA_DELETE')"
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {

        bodegaService.eliminar(id);

        return ResponseEntity.noContent().build();
    }
}
