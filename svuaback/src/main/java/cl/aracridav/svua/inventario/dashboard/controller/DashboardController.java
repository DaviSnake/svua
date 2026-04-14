package cl.aracridav.svua.inventario.dashboard.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.aracridav.svua.inventario.dashboard.dto.response.DashboardResponse;
import cl.aracridav.svua.inventario.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/svua/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService service;

    @GetMapping("/full")
    public ResponseEntity<DashboardResponse> obtenerDashboard() {
        return ResponseEntity.ok(service.obtenerDashboard());
    }

}
