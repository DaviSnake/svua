package cl.aracridav.svua.mantenimiento.orden.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import cl.aracridav.svua.mantenimiento.orden.dto.request.ActualizarOrdenMantenimientoRequest;
import cl.aracridav.svua.mantenimiento.orden.dto.request.OrdenMantenimientoRequest;
import cl.aracridav.svua.mantenimiento.orden.dto.request.ReprogramarOrdenRequest;
import cl.aracridav.svua.mantenimiento.orden.dto.response.OrdenEjecucionResponse;
import cl.aracridav.svua.mantenimiento.orden.dto.response.OrdenMantenimientoResponse;
import cl.aracridav.svua.mantenimiento.orden.service.OrdenMantenimientoService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/svua/ordenes-mantenimiento")
@RequiredArgsConstructor
public class OrdenMantenimientoController {

    private final OrdenMantenimientoService ordenMantenimientoService;

    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA') or " +
        "(hasAuthority('ORDEN_MANT_CREATE')) "
    )
    @PostMapping
    public ResponseEntity<OrdenMantenimientoResponse> crearOrden(
            @RequestBody OrdenMantenimientoRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ordenMantenimientoService.crearOrden(request));
    }

    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA') or " +
        "(hasAuthority('ORDEN_MANT_VIEW')) "
    )
    @GetMapping
    public ResponseEntity<List<OrdenMantenimientoResponse>> listar() {
        return ResponseEntity.ok(ordenMantenimientoService.listarOrdenesEmpresa());
    }

    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA') or " +
        "(hasAuthority('ORDEN_MANT_VIEW')) "
    )
    @PutMapping("/{id}/cancelar")
        public ResponseEntity<Void> cancelar(
            @PathVariable Long id,
            @RequestParam String motivo,
            @RequestParam Long usuarioId
    ) {
        ordenMantenimientoService.cancelarOrden(id, motivo, usuarioId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA') or " +
        "(hasAuthority('ORDEN_MANT_UPDATE')) "
    )
    @PutMapping("/{ordenId}")
    public ResponseEntity<OrdenMantenimientoResponse> actualizarOrden(
        @PathVariable Long ordenId,
        @RequestBody ActualizarOrdenMantenimientoRequest request) {
        return ResponseEntity.ok(ordenMantenimientoService.actualizar(ordenId, request));
    }

    @PutMapping("/{id}/ejecutar")
    public ResponseEntity<OrdenEjecucionResponse> ejecutarOrden(@PathVariable Long id) {

        OrdenEjecucionResponse orden = ordenMantenimientoService.ejecutarOrden(id);

        return ResponseEntity.ok(orden);
    }

    @PutMapping("/{id}/detener")
    public ResponseEntity<OrdenEjecucionResponse> detenerOrden(@PathVariable Long id) {

        OrdenEjecucionResponse orden = ordenMantenimientoService.detenerOrden(id);

        return ResponseEntity.ok(orden);
    }

    @PostMapping("/{id}/detenerConArchivo")
    public ResponseEntity<Void> detener(
            @PathVariable Long id,
            @RequestParam("archivo") MultipartFile archivo) {

        ordenMantenimientoService.detenerOrden(id, archivo);

        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/reprogramar")
    public ResponseEntity<OrdenMantenimientoResponse> reprogramarOrden(
            @PathVariable Long id,
            @RequestBody ReprogramarOrdenRequest request
    ) {
        return ResponseEntity.ok(
            ordenMantenimientoService.reprogramarOrden(id, request.getNuevaFecha(), request.getMotivo())
        );
    }

}
