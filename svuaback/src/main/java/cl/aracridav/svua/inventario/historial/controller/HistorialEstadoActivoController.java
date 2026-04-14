package cl.aracridav.svua.inventario.historial.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.aracridav.svua.inventario.activo.entity.Activo;
import cl.aracridav.svua.inventario.activo.repository.ActivoRepository;
import cl.aracridav.svua.inventario.historial.dto.response.HistorialEstadoActivoResponse;
import cl.aracridav.svua.inventario.historial.service.HistorialEstadoActivoService;
import cl.aracridav.svua.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/svua/historial-activo")
@RequiredArgsConstructor
public class HistorialEstadoActivoController {

    private final HistorialEstadoActivoService service;
    private final ActivoRepository activoRepository;

    @PreAuthorize(
        "hasRole('SUPER_ADMIN') or " +
        "(hasAuthority('HISTORIAL_VIEW')) "
    )
    @GetMapping("/{activoId}")
    public ResponseEntity<List<HistorialEstadoActivoResponse>> obtenerHistorial(
            @PathVariable Long activoId
    ) {

        Activo activo = activoRepository.findById(activoId)
                .orElseThrow(() -> new BusinessException("Activo no encontrado"));

        return ResponseEntity.ok(service.obtenerHistorial(activo.getId()));
    }

}
