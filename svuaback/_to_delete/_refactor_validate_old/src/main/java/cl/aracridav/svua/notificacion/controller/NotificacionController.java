package cl.aracridav.svua.notificacion.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.aracridav.svua.notificacion.dto.response.NotificacionResponse;
import cl.aracridav.svua.notificacion.service.NotificacionService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/svua/notificacion")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;

    @GetMapping
    public List<NotificacionResponse> listar() {
        return notificacionService.listar();
    }

    @GetMapping("/no-leidas/count/{empresaId}")
    public Long contarNoLeidas(
            @PathVariable Long empresaId) {
        return notificacionService.contarNoLeidas(empresaId);
    }

    @PutMapping("/{id}/leer")
    public ResponseEntity<Void> marcarComoLeida(
            @PathVariable Long id) {

        notificacionService.marcarComoLeida(id);

        return ResponseEntity.ok().build();
    }

    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA')"
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id) {

        notificacionService.eliminar(id);

        return ResponseEntity.noContent().build();
    }

}
