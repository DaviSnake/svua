package cl.aracridav.svua.usuario.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.aracridav.svua.config.security.JwtService;
import cl.aracridav.svua.usuario.dto.request.ActualizarActividadRequest;
import cl.aracridav.svua.usuario.dto.response.SesionUsuarioResponse;
import cl.aracridav.svua.usuario.repository.SesionUsuarioRepository;
import cl.aracridav.svua.usuario.service.SesionUsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/svua/sesiones")
public class SesionUsuarioController {

    private final SesionUsuarioRepository sesionRepository;
    private final SesionUsuarioService sesionUsuarioService;
    private final JwtService jwtService;

    @PostMapping("/actividad")
    public ResponseEntity<Void> actualizarActividad(
            @RequestBody ActualizarActividadRequest request,
            HttpServletRequest httpRequest) {

        String jwt =
            httpRequest.getHeader("Authorization")
                .substring(7);

        String tokenJti =
            jwtService.extractTokenJti(jwt);

        sesionUsuarioService.actualizarActividad(
            tokenJti,
            request.getPagina(),
            request.getAccion()
        );

        return ResponseEntity.ok().build();
    }

    @GetMapping("/activas")
    public List<SesionUsuarioResponse> obtenerSesionesActivas() {

        return sesionRepository.findActivasConUsuarioEmpresa()
            .stream()
            .map(s -> SesionUsuarioResponse.builder()
                .id(s.getId())
                .usuarioId(s.getUsuario().getId())
                .usuario(s.getUsuario().getNombre())
                .empresa(s.getEmpresa().getNombre())
                .fechaLogin(s.getFechaLogin())
                .ultimaActividad(s.getUltimaActividad())
                .paginaActual(s.getPaginaActual())
                .ultimaAccion(s.getUltimaAccion())
                .ip(s.getIp())
                .navegador(s.getNavegador())
                .sistemaOperativo(s.getSistemaOperativo())
                .dispositivo(s.getDispositivo())
                .cantidadRequests(
                    s.getCantidadRequests())
                .activa(s.getActiva())
                .build())
        .toList();
    }

    // 🔥 Informe de conexiones: historial completo, paginado y filtrable,
    // visible solo para SUPER_ADMIN.
    @GetMapping("/historial")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Page<SesionUsuarioResponse> obtenerHistorial(
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) Long empresaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);

        return sesionUsuarioService
            .obtenerHistorial(usuario, empresaId, fecha, pageable)
            .map(s -> SesionUsuarioResponse.builder()
                .id(s.getId())
                .usuarioId(s.getUsuario().getId())
                .usuario(s.getUsuario().getNombre())
                .empresa(s.getEmpresa().getNombre())
                .fechaLogin(s.getFechaLogin())
                .ultimaActividad(s.getUltimaActividad())
                .paginaActual(s.getPaginaActual())
                .ultimaAccion(s.getUltimaAccion())
                .ip(s.getIp())
                .navegador(s.getNavegador())
                .sistemaOperativo(s.getSistemaOperativo())
                .dispositivo(s.getDispositivo())
                .cantidadRequests(
                    s.getCantidadRequests())
                .activa(s.getActiva())
                .build());
    }

    @PostMapping("/logout/{tokenJti}")
    public ResponseEntity<Void> logout(
         @PathVariable String tokenJti) {

        sesionUsuarioService.cerrarSesion(tokenJti);

        return ResponseEntity.ok().build();
    }

}
