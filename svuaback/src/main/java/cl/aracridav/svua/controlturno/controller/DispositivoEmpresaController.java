package cl.aracridav.svua.controlturno.controller;

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

import cl.aracridav.svua.controlturno.dto.request.DispositivoEmpresaRequest;
import cl.aracridav.svua.controlturno.dto.response.DispositivoEmpresaResponse;
import cl.aracridav.svua.controlturno.service.DispositivoEmpresaService;
import lombok.RequiredArgsConstructor;

// 🔒 Solo SUPER_ADMIN: define que dispositivo fisico de monitoreo
// alimenta a que empresa (ver DispositivoEmpresa/CorreoLecturaImportador).
@RestController
@RequestMapping("/api/v1/svua/control-turno/dispositivos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class DispositivoEmpresaController {

    private final DispositivoEmpresaService service;

    @PostMapping
    public ResponseEntity<DispositivoEmpresaResponse> registrar(@RequestBody DispositivoEmpresaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrar(request));
    }

    @GetMapping
    public ResponseEntity<Page<DispositivoEmpresaResponse>> listar(
            Pageable pageable,
            @RequestParam(required = false) Long empresaId,
            @RequestParam(required = false) String busqueda) {
        return ResponseEntity.ok(service.listar(pageable, empresaId, busqueda));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DispositivoEmpresaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DispositivoEmpresaResponse> actualizar(
            @PathVariable Long id, @RequestBody DispositivoEmpresaRequest request) {
        return ResponseEntity.ok(service.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/habilitar")
    public ResponseEntity<Void> habilitar(@PathVariable Long id) {
        service.habilitar(id);
        return ResponseEntity.noContent().build();
    }
}
