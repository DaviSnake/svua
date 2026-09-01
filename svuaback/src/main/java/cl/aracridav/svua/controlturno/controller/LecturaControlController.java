package cl.aracridav.svua.controlturno.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import cl.aracridav.svua.controlturno.dto.request.LecturaControlRequest;
import cl.aracridav.svua.controlturno.dto.response.ImportHojaControlResponse;
import cl.aracridav.svua.controlturno.dto.response.LecturaControlResponse;
import cl.aracridav.svua.controlturno.dto.response.PuntoControlDashboardResponse;
import cl.aracridav.svua.controlturno.enums.TurnoTrabajo;
import cl.aracridav.svua.controlturno.service.HojaControlImportService;
import cl.aracridav.svua.controlturno.service.LecturaControlService;
import lombok.RequiredArgsConstructor;

// 🔥 Registro de lecturas horarias por turno (temperatura, humedad,
// setpoint, etc.) para cada PuntoControl, y su dashboard de graficos --
// reemplaza a la planilla Excel "SISTEMA_DE_CONTROL_DE_MANTENCION".
@RestController
@RequestMapping("/api/v1/svua/control-turno/lecturas")
@RequiredArgsConstructor
public class LecturaControlController {

    private final LecturaControlService service;
    private final HojaControlImportService importService;

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA','JEFE_MANTENIMIENTO','TECNICO')")
    @PostMapping
    public ResponseEntity<LecturaControlResponse> registrar(@RequestBody LecturaControlRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrar(request));
    }

    // 🔥 Carga masiva desde la planilla real "HOJA DE CONTROL": crea las
    // lecturas de HOY para cada punto/hora que trae el archivo (ver
    // HojaControlImportServiceImpl). Sin TECNICO: a diferencia de
    // registrar() una sola lectura, esto puede crear puntos de control
    // nuevos en el catalogo, lo que hoy es una accion de administracion.
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA','JEFE_MANTENIMIENTO')")
    @PostMapping("/importar-excel")
    public ResponseEntity<ImportHojaControlResponse> importarExcel(
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(importService.importar(file));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA','JEFE_MANTENIMIENTO','TECNICO')")
    @GetMapping
    public ResponseEntity<Page<LecturaControlResponse>> listar(
            Pageable pageable,
            @RequestParam(required = false) Long puntoControlId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(required = false) TurnoTrabajo turno) {
        return ResponseEntity.ok(service.listar(pageable, puntoControlId, desde, hasta, turno));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA','JEFE_MANTENIMIENTO','TECNICO')")
    @GetMapping("/dashboard")
    public ResponseEntity<List<PuntoControlDashboardResponse>> dashboard(
            @RequestParam(required = false) Long puntoControlId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(required = false) TurnoTrabajo turno) {
        return ResponseEntity.ok(service.dashboard(puntoControlId, desde, hasta, turno));
    }
}
