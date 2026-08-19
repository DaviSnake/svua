package cl.aracridav.svua.inventario.historial.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.aracridav.svua.inventario.activo.entity.Activo;
import cl.aracridav.svua.inventario.activo.repository.ActivoRepository;
import cl.aracridav.svua.inventario.historial.dto.response.HistorialActivoCompletoResponse;
import cl.aracridav.svua.inventario.historial.dto.response.HistorialEstadoActivoResponse;
import cl.aracridav.svua.inventario.historial.service.HistorialEstadoActivoService;
import cl.aracridav.svua.shared.exception.BusinessException;
import cl.aracridav.svua.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/svua/historial-activo")
@RequiredArgsConstructor
public class HistorialEstadoActivoController {

    private final HistorialEstadoActivoService service;
    private final ActivoRepository activoRepository;

    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA') or " +
        "(hasAuthority('HISTORIAL_VIEW')) "
    )
    @GetMapping("/{activoId}")
    public ResponseEntity<List<HistorialEstadoActivoResponse>> obtenerHistorial(
            @PathVariable Long activoId
    ) {

        Activo activo = activoRepository.findById(activoId)
                .orElseThrow(() -> new BusinessException("Activo no encontrado"));

        // 🔐 Validación multi-tenant
        if (!SecurityUtils.esSuperAdmin()
                && !activo.getEmpresa().getId().equals(SecurityUtils.getEmpresaId())) {
            throw new BusinessException("No pertenece a esta empresa");
        }

        return ResponseEntity.ok(service.obtenerHistorial(activo.getId()));
    }

    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA') or " +
        "(hasAuthority('HISTORIAL_VIEW')) "
    )
    @GetMapping("/historial")
    public ResponseEntity<List<HistorialActivoCompletoResponse>>
    obtenerHistorialTodos(
            @RequestParam(required = false) Long empresaId) {

        // 🔥 empresaId es opcional: permite filtrar la auditoría por
        // empresa (solo tiene efecto para SUPER_ADMIN, ver
        // HistorialEstadoActivoServiceImpl).
        return ResponseEntity.ok(
            service.obtenerHistorialCompletoTodos(empresaId)
        );
    }

    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA') or " +
        "(hasAuthority('HISTORIAL_VIEW')) "
    )
    @GetMapping("/{id}/historial")
    public ResponseEntity<HistorialActivoCompletoResponse>
    obtenerHistorialCompleto(
            @PathVariable Long id) {

        HistorialActivoCompletoResponse response =
            service.obtenerHistorialCompleto(id);

        return ResponseEntity.ok(response);
    }

}
