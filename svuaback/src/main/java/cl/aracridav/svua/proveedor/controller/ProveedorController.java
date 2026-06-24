package cl.aracridav.svua.proveedor.controller;

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

import cl.aracridav.svua.proveedor.dto.request.ProveedorCreateRequest;
import cl.aracridav.svua.proveedor.dto.request.ProveedorUpdateRequest;
import cl.aracridav.svua.proveedor.dto.response.ProveedorResponse;
import cl.aracridav.svua.proveedor.service.ProveedorService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(value = "/api/v1/svua/proveedores")
@RequiredArgsConstructor
public class ProveedorController {

    private final ProveedorService proveedorService;

    /*
     * =========================================
     * CREAR
     * =========================================
     */
    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA') or " +
        "hasAuthority('PROVEEDOR_CREATE')"
    )
    @PostMapping
    public ResponseEntity<ProveedorResponse> registrarProveedor(
            @RequestBody ProveedorCreateRequest request) {

        ProveedorResponse response = proveedorService.registrarProveedor(request);

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
        "hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA','TECNICO') or " +
        "hasAuthority('PROVEEDOR_VIEW')"
    )
    @GetMapping
    public ResponseEntity<Page<ProveedorResponse>> listarProveedores(Pageable pageable) {

        Page<ProveedorResponse> response = proveedorService.listarProveedores(pageable);

        return ResponseEntity.ok(response);
    }

    /*
     * =========================================
     * OBTENER POR ID
     * =========================================
     */
    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA') or " +
        "hasAuthority('PROVEEDOR_VIEW')"
    )
    @GetMapping("/{id}")
    public ResponseEntity<ProveedorResponse> obtener(@PathVariable Long id) {

        ProveedorResponse response = proveedorService.obtener(id);

        return ResponseEntity.ok(response);
    }

    /*
     * =========================================
     * ACTUALIZAR
     * =========================================
     */
    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA') or " +
        "hasAuthority('PROVEEDOR_UPDATE')"
    )
    @PutMapping("/{id}")
    public ResponseEntity<ProveedorResponse> actualizar(
            @PathVariable Long id,
            @RequestBody ProveedorUpdateRequest request) {

        ProveedorResponse response = proveedorService.actualizar(id, request);

        return ResponseEntity.ok(response);
    }

    /*
     * =========================================
     * ELIMINAR (SOFT DELETE)
     * =========================================
     */
    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA') or " +
        "hasAuthority('PROVEEDOR_DELETE')"
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {

        proveedorService.eliminar(id);

        return ResponseEntity.noContent().build();
    }
}
