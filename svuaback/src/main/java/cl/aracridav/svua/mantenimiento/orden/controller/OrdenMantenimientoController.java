package cl.aracridav.svua.mantenimiento.orden.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import cl.aracridav.svua.mantenimiento.orden.dto.response.CostosGraficoReponse;
import cl.aracridav.svua.mantenimiento.orden.dto.response.OrdenEjecucionResponse;
import cl.aracridav.svua.mantenimiento.orden.dto.response.OrdenMantenimientoReporteResponse;
import cl.aracridav.svua.mantenimiento.orden.dto.response.OrdenMantenimientoResponse;
import cl.aracridav.svua.mantenimiento.orden.entity.EstadoOrden;
import cl.aracridav.svua.mantenimiento.orden.service.OrdenMantenimientoService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/svua/ordenes-mantenimiento")
@RequiredArgsConstructor
public class OrdenMantenimientoController {

    private final OrdenMantenimientoService ordenMantenimientoService;

    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA','TECNICO') or " +
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
    public ResponseEntity<List<OrdenMantenimientoResponse>> listar(
            @RequestParam(required = false) Long empresaId) {
        return ResponseEntity.ok(ordenMantenimientoService.listarOrdenesEmpresa(empresaId));
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
        "hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA','TECNICO') or " +
        "(hasAuthority('ORDEN_MANT_UPDATE')) "
    )
    @PutMapping("/{ordenId}")
    public ResponseEntity<OrdenMantenimientoResponse> actualizarOrden(
        @PathVariable Long ordenId,
        @RequestBody ActualizarOrdenMantenimientoRequest request) {
        return ResponseEntity.ok(ordenMantenimientoService.actualizar(ordenId, request));
    }

    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA','TECNICO') or " +
        "(hasAuthority('ORDEN_MANT_UPDATE')) "
    )
    @PutMapping("/{id}/ejecutar")
    public ResponseEntity<OrdenEjecucionResponse> ejecutarOrden(@PathVariable Long id) {

        OrdenEjecucionResponse orden = ordenMantenimientoService.ejecutarOrden(id);

        return ResponseEntity.ok(orden);
    }

    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA') or " +
        "(hasAuthority('ORDEN_MANT_UPDATE')) "
    )
    @PutMapping("/{id}/detener")
    public ResponseEntity<OrdenEjecucionResponse> detenerOrden(@PathVariable Long id) {

        OrdenEjecucionResponse orden = ordenMantenimientoService.detenerOrden(id);

        return ResponseEntity.ok(orden);
    }

    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA','TECNICO') or " +
        "(hasAuthority('ORDEN_MANT_UPDATE')) "
    )
    @PostMapping("/{id}/preDetenerConArchivo")
    public ResponseEntity<Void> detener(
            @PathVariable Long id,
            // 🔥 el checklist/archivo es opcional: se puede terminar la
            // ejecución sin ingresarlo (queda pendiente durante las 24h
            // siguientes en PRE_COMPLETADA).
            @RequestParam(value = "archivo", required = false) MultipartFile archivo) {

        ordenMantenimientoService.preDetenerOrden(id, archivo);

        return ResponseEntity.ok().build();
    }

    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA','TECNICO') or " +
        "(hasAuthority('ORDEN_MANT_UPDATE')) "
    )
    @PostMapping("/{id}/subirChecklist")
    public ResponseEntity<OrdenEjecucionResponse> subirChecklist(
            @PathVariable Long id,
            @RequestParam("archivo") MultipartFile archivo) {

        OrdenEjecucionResponse orden =
                ordenMantenimientoService.subirChecklist(id, archivo);

        return ResponseEntity.ok(orden);
    }

    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA','TECNICO') or " +
        "(hasAuthority('ORDEN_MANT_UPDATE')) "
    )
    @GetMapping("/{id}/archivo")
    public ResponseEntity<Resource> verArchivo(
            @PathVariable Long id) throws IOException {

        Resource resource =
                ordenMantenimientoService.obtenerArchivo(id);

        String contentType =
                Files.probeContentType(
                        Paths.get(resource.getFile().getAbsolutePath()));

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA','TECNICO') or " +
        "(hasAuthority('ORDEN_MANT_UPDATE')) "
    )
    @PutMapping("/{id}/reprogramar")
    public ResponseEntity<OrdenMantenimientoResponse> reprogramarOrden(
            @PathVariable Long id,
            @RequestBody ReprogramarOrdenRequest request
    ) {
        return ResponseEntity.ok(
            ordenMantenimientoService.reprogramarOrden(id, request.getNuevaFecha(), request.getMotivo())
        );
    }

    // 🔒 empresaId es opcional y solo tiene efecto si quien llama es
    // SUPER_ADMIN (ver OrdenMantenimientoServiceImpl).
    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA') or " +
        "(hasAuthority('ORDEN_MANT_VIEW')) "
    )
    @GetMapping("/grafico/costos")
    public ResponseEntity<CostosGraficoReponse> obtenerGraficoCostos(
            @RequestParam(required = false) Long activoId,
            @RequestParam(required = false) Long empresaId) {

        return ResponseEntity.ok(
            ordenMantenimientoService
                .obtenerGraficoCostosUltimos6Meses(activoId, empresaId)
        );
    }

    // 🔥 Informe de Mantenciones: historial paginado y filtrable de
    // ordenes completadas, visible para SUPER_ADMIN y ADMIN_EMPRESA. El
    // aislamiento por empresa para ADMIN_EMPRESA no depende de este
    // filtro: lo garantiza Row Level Security de Postgres (ver
    // V27__enable_row_level_security_por_empresa.sql), que restringe
    // automaticamente cualquier query sobre orden_mantenimiento a la
    // empresa del usuario autenticado salvo que sea SUPER_ADMIN.
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA')")
    @GetMapping("/informe")
    public Page<OrdenMantenimientoReporteResponse> obtenerInformeMantenciones(
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) String orden,
            @RequestParam(required = false) Long empresaId,
            @RequestParam(required = false) EstadoOrden estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);

        return ordenMantenimientoService
            .obtenerInformeMantenciones(usuario, orden, empresaId, estado, fecha, pageable);
    }

}
