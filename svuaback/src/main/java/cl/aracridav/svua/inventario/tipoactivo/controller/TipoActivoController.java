package cl.aracridav.svua.inventario.tipoactivo.controller;

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

import cl.aracridav.svua.inventario.tipoactivo.dto.request.TipoActivoCreateRequest;
import cl.aracridav.svua.inventario.tipoactivo.dto.response.TipoActivoResponse;
import cl.aracridav.svua.inventario.tipoactivo.service.TipoActivoService;
import cl.aracridav.svua.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(value = "/api/v1/svua/tipos-activo")
@RequiredArgsConstructor
public class TipoActivoController {

     private final TipoActivoService tipoActivoService;

    @PreAuthorize(
        "hasRole('SUPER_ADMIN') or " +
        "(hasAuthority('TIPO_ACTIVO_CREATE')) "
    )
    @PostMapping
    public ResponseEntity<TipoActivoResponse> crear(@RequestBody TipoActivoCreateRequest request) {

        Long empresaId = SecurityUtils.getEmpresaId();

        TipoActivoResponse response = tipoActivoService.crear(empresaId, request);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }

    @PreAuthorize(
        "hasRole('SUPER_ADMIN') or " +
        "(hasAuthority('TIPO_ACTIVO_VIEW')) "
    )
    @GetMapping
    public ResponseEntity<Page<TipoActivoResponse>> listarTipoActivos(Pageable pegable) {

        Page<TipoActivoResponse> response = tipoActivoService.listarTipoActivos(pegable);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

}
