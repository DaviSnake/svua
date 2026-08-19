package cl.aracridav.svua.inventario.tipoactivo.controller;

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

import cl.aracridav.svua.inventario.tipoactivo.dto.request.TipoActivoCreateRequest;
import cl.aracridav.svua.inventario.tipoactivo.dto.response.TipoActivoResponse;
import cl.aracridav.svua.inventario.tipoactivo.service.TipoActivoService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(value = "/api/v1/svua/tipos-activo")
@RequiredArgsConstructor
public class TipoActivoController {

    private final TipoActivoService tipoActivoService;

    /*
     * =========================================
     * CREAR
     * =========================================
     */
    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA') or hasAuthority('TIPO_ACTIVO_CREATE')"
    )
    @PostMapping
    public ResponseEntity<TipoActivoResponse> crear(
            @RequestBody TipoActivoCreateRequest request) {

        TipoActivoResponse response = tipoActivoService.crear(request);

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
        "hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA') or " +
        "(hasAuthority('TIPO_ACTIVO_VIEW')) "
    )
    @GetMapping
    public ResponseEntity<Page<TipoActivoResponse>> listarTipoActivos(
            Pageable pageable,
            @RequestParam(required = false) Long empresaId,
            @RequestParam(required = false) String busqueda) {

        // 🔥 empresaId es opcional: permite filtrar la grilla por empresa
        // (solo tiene efecto para SUPER_ADMIN, ver TipoActivoServiceImpl).
        // 🔥 busqueda es opcional: filtra por nombre.
        Page<TipoActivoResponse> response =
                tipoActivoService.listarTipoActivos(pageable, empresaId, busqueda);

        return ResponseEntity.ok(response); // ✅ FIX
    }

    /*
     * =========================================
     * OBTENER POR ID
     * =========================================
     */
    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA') or hasAuthority('TIPO_ACTIVO_VIEW')"
    )
    @GetMapping("/{id}")
    public ResponseEntity<TipoActivoResponse> obtener(@PathVariable Long id) {

        TipoActivoResponse response = tipoActivoService.obtener(id);

        return ResponseEntity.ok(response);
    }

    /*
     * =========================================
     * ACTUALIZAR
     * =========================================
     */
    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA') or hasAuthority('TIPO_ACTIVO_UPDATE')"
    )
    @PutMapping("/{id}")
    public ResponseEntity<TipoActivoResponse> actualizar(
            @PathVariable Long id,
            @RequestBody TipoActivoCreateRequest request) {

        TipoActivoResponse response =
                tipoActivoService.actualizar(id, request);

        return ResponseEntity.ok(response);
    }

    /*
     * =========================================
     * ELIMINAR (SOFT DELETE)
     * =========================================
     */
    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA') or hasAuthority('TIPO_ACTIVO_DELETE')"
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {

        tipoActivoService.eliminar(id);

        return ResponseEntity.noContent().build(); // ✅ 204 correcto
    }
}
