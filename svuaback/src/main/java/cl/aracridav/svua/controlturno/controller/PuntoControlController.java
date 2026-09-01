package cl.aracridav.svua.controlturno.controller;

import java.util.List;

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

import cl.aracridav.svua.controlturno.dto.request.PuntoControlRequest;
import cl.aracridav.svua.controlturno.dto.response.PuntoControlResponse;
import cl.aracridav.svua.controlturno.service.PuntoControlService;
import lombok.RequiredArgsConstructor;

// 🔥 Catalogo de "puntos de control" (ej. Camara de Fermentacion,
// Horno, Sala de Proceso) que se monitorean manualmente cada turno --
// ver LecturaControlController para el registro de lecturas y el
// dashboard de graficos. Reemplaza el registro en planilla Excel
// (SISTEMA_DE_CONTROL_DE_MANTENCION) por una pantalla dentro de svua.
@RestController
@RequestMapping("/api/v1/svua/control-turno/puntos")
@RequiredArgsConstructor
public class PuntoControlController {

    private final PuntoControlService service;

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA')")
    @PostMapping
    public ResponseEntity<PuntoControlResponse> registrar(@RequestBody PuntoControlRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrar(request));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA')")
    @GetMapping
    public ResponseEntity<Page<PuntoControlResponse>> listar(
            Pageable pageable,
            @RequestParam(required = false) Long empresaId,
            @RequestParam(required = false) String busqueda) {
        return ResponseEntity.ok(service.listar(pageable, empresaId, busqueda));
    }

    // 🔥 Combo simple (sin paginar) para el formulario de ingreso de
    // lecturas: cualquier rol que registra datos de turno necesita ver
    // los puntos activos de su empresa, no solo quien administra el
    // catalogo.
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA','JEFE_MANTENIMIENTO','TECNICO')")
    @GetMapping("/activos")
    public ResponseEntity<List<PuntoControlResponse>> listarActivos() {
        return ResponseEntity.ok(service.listarActivos());
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA')")
    @GetMapping("/{id}")
    public ResponseEntity<PuntoControlResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA')")
    @PutMapping("/{id}")
    public ResponseEntity<PuntoControlResponse> actualizar(
            @PathVariable Long id, @RequestBody PuntoControlRequest request) {
        return ResponseEntity.ok(service.actualizar(id, request));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // 🔥 Reactiva un punto de control deshabilitado (contraparte de
    // eliminar(), que solo hace soft-delete con activo=false).
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA')")
    @PutMapping("/{id}/habilitar")
    public ResponseEntity<Void> habilitar(@PathVariable Long id) {
        service.habilitar(id);
        return ResponseEntity.noContent().build();
    }
}
