package cl.aracridav.svua.proveedor.controller;

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

import cl.aracridav.svua.proveedor.dto.request.ProveedorCreateRequest;
import cl.aracridav.svua.proveedor.dto.response.ProveedorResponse;
import cl.aracridav.svua.proveedor.service.ProveedorService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(value = "/api/v1/svua/proveedores")
@RequiredArgsConstructor
public class ProveedorController {

    private final ProveedorService proveedorService;

    @PreAuthorize(
        "hasRole('SUPER_ADMIN') or " +
        "(hasAuthority('PROVEEDOR_CREATE')) "
    )
    @PostMapping
    public ResponseEntity<ProveedorResponse> registrarProveedor(
            @RequestBody ProveedorCreateRequest request) {

        ProveedorResponse response = proveedorService.registrarProveedor(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PreAuthorize(
        "hasRole('SUPER_ADMIN') or " +
        "(hasAuthority('PROVEEDOR_VIEW')) "
    )
    @GetMapping
    public ResponseEntity<Page<ProveedorResponse>> listarProveedores(Pageable pegable) {

        Page<ProveedorResponse> response = proveedorService.listarProveedores(pegable);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

}
