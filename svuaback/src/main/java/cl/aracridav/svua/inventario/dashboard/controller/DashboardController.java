package cl.aracridav.svua.inventario.dashboard.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.aracridav.svua.inventario.dashboard.dto.response.DashboardIndicadoresResponse;
import cl.aracridav.svua.inventario.dashboard.dto.response.DashboardResponse;
import cl.aracridav.svua.inventario.dashboard.dto.response.IndicadorCumplimientoResponse;
import cl.aracridav.svua.inventario.dashboard.dto.response.IndicadorMTTRResponse;
import cl.aracridav.svua.inventario.dashboard.service.DashboardService;
import cl.aracridav.svua.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/svua/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/full")
    public ResponseEntity<DashboardResponse> obtenerDashboard() {
        return ResponseEntity.ok(dashboardService.obtenerDashboard());
    }

    @GetMapping("/cumplimiento/semanal")
    public ResponseEntity<IndicadorCumplimientoResponse> obtenerIndicador() {

        Long empresaId = SecurityUtils.getEmpresaId();

        return ResponseEntity.ok(
            dashboardService
                .obtenerIndicadorSemanal(empresaId));
    }

    @GetMapping("/cumplimiento/mensual")
    public ResponseEntity<IndicadorCumplimientoResponse>
    obtenerIndicadorMensual() {

        Long empresaId =
                SecurityUtils.getEmpresaId();

        return ResponseEntity.ok(
            dashboardService.obtenerIndicadorMensual(empresaId));
    }

    @GetMapping("/mttr")
    public ResponseEntity<IndicadorMTTRResponse>
    obtenerMTTR() {

        Long empresaId = SecurityUtils.getEmpresaId();

        return ResponseEntity.ok(
            dashboardService
                .obtenerMTTRMensual(empresaId));
    }

    @GetMapping
    public ResponseEntity<DashboardIndicadoresResponse> getDashboard() {

        Long empresaId = SecurityUtils.getEmpresaId();

        return ResponseEntity.ok(
                dashboardService.obtenerDashboard(empresaId)
        );
    }

}
