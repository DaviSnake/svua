package cl.aracridav.svua.usuario.controller;

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

import cl.aracridav.svua.auth.dto.request.ChangePasswordRequest;
import cl.aracridav.svua.usuario.dto.request.RegisterRequest;
import cl.aracridav.svua.usuario.dto.request.UpdateUsuarioRequest;
import cl.aracridav.svua.usuario.dto.response.UsuarioResponse;
import cl.aracridav.svua.usuario.service.UsuarioService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/svua/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PreAuthorize(
        "hasRole('SUPER_ADMIN') or " +
        "(hasAuthority('USUARIO_CREATE')) "
    )
    @PostMapping
    public ResponseEntity<UsuarioResponse> register(@RequestBody RegisterRequest request) {
        
        UsuarioResponse response = usuarioService.registrarUsuario(request);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }

    @PreAuthorize(
        "hasRole('SUPER_ADMIN') or " +
        "(hasAuthority('USUARIO_DELETE'))"
    )
    @DeleteMapping("/{usuarioId}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long usuarioId) {

        usuarioService.eliminarUsuario(usuarioId);

        return ResponseEntity.noContent().build();   

    }

    @PreAuthorize(
        "hasRole('SUPER_ADMIN') or " +
        "(hasAuthority('USUARIO_UPDATE'))"
    )
    @PutMapping("/{usuarioId}")
    public ResponseEntity<UsuarioResponse> actualizar(
            @PathVariable Long usuarioId,
            @RequestBody UpdateUsuarioRequest request) {

        UsuarioResponse response =
                usuarioService.actualizarUsuario(usuarioId, request);

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{usuarioId}/password")
    public ResponseEntity<?> cambiarPassword(
            @PathVariable Long usuarioId,
            @RequestBody ChangePasswordRequest request) {
        
        usuarioService.cambiarPassword(usuarioId, request);

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize(
        "hasRole('SUPER_ADMIN') or " +
        "(hasAuthority('USUARIO_VIEW'))"
    )
    @GetMapping
    public ResponseEntity<Page<UsuarioResponse>> listarUsuarios(Pageable pegable) {

        return ResponseEntity.ok(usuarioService.listarUsuarios(pegable));
    }
}
