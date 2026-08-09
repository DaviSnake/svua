package cl.aracridav.svua.shared.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import cl.aracridav.svua.inventario.activo.dto.request.ActivoImportRowDTO;
import cl.aracridav.svua.inventario.activo.dto.response.ActivoImportResultDTO;
import cl.aracridav.svua.inventario.tipoactivo.dto.request.TipoActivoImportRowDTO;
import cl.aracridav.svua.inventario.ubicacion.dto.request.UbicacionImportRowDTO;
import cl.aracridav.svua.mantenimiento.orden.dto.request.OrdenImportRowDTO;
import cl.aracridav.svua.mantenimiento.repuesto.dto.request.RepuestoImportRowDTO;
import cl.aracridav.svua.proveedor.dto.request.ProveedorImportRowDTO;
import cl.aracridav.svua.shared.dto.response.ImportBatchResultDTO;
import cl.aracridav.svua.shared.dto.response.ImportProgressDTO;
import cl.aracridav.svua.shared.service.ExcelImportService;
import cl.aracridav.svua.shared.service.ImportProgressService;
import cl.aracridav.svua.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/svua/import")
@RequiredArgsConstructor
public class ExcelController {

    private final ExcelImportService service;
    private final ImportProgressService progressService;

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    @PostMapping(("/{archivo}"))
    public ResponseEntity<Map<String, String>> upload(@PathVariable String archivo, @RequestParam("file") MultipartFile file) throws IOException {

        Long empresaId = SecurityUtils.getEmpresaId();
        Long usuarioId = SecurityUtils.getUsuarioId();

        String jobId = UUID.randomUUID().toString();
        Path tempFile = Files.createTempFile("upload-", ".xlsx");
        file.transferTo(tempFile.toFile());

        service.procesarAsync(tempFile, jobId, empresaId, usuarioId, archivo);

        return ResponseEntity.ok(Map.of("jobId", jobId));
    }

    @GetMapping("/progress/{jobId}")
    public ResponseEntity<ImportProgressDTO> progreso(@PathVariable String jobId) {
        return ResponseEntity.ok(progressService.get(jobId));
    }

    // ==========================================================
    // 🔥 Ingreso en tiempo real (grilla tipo planilla) por entidad
    // ==========================================================

    /**
     * Ingreso en tiempo real (grilla tipo planilla) de Activos: recibe un
     * arreglo JSON de filas ya tipadas (en vez de un archivo Excel), las
     * valida y guarda de forma síncrona, y devuelve el detalle fila por fila.
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    @PostMapping("/activo/manual")
    public ResponseEntity<ActivoImportResultDTO> subirActivosManual(@RequestBody List<ActivoImportRowDTO> filas) {

        Long empresaId = SecurityUtils.getEmpresaId();
        Long usuarioId = SecurityUtils.getUsuarioId();

        ActivoImportResultDTO resultado = service.procesarActivosManual(filas, empresaId, usuarioId);

        return ResponseEntity.ok(resultado);
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    @PostMapping("/proveedor/manual")
    public ResponseEntity<ImportBatchResultDTO> subirProveedoresManual(@RequestBody List<ProveedorImportRowDTO> filas) {

        Long empresaId = SecurityUtils.getEmpresaId();

        return ResponseEntity.ok(service.procesarProveedoresManual(filas, empresaId));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    @PostMapping("/orden/manual")
    public ResponseEntity<ImportBatchResultDTO> subirOrdenesManual(@RequestBody List<OrdenImportRowDTO> filas) {

        Long empresaId = SecurityUtils.getEmpresaId();
        Long usuarioId = SecurityUtils.getUsuarioId();

        return ResponseEntity.ok(service.procesarOrdenesManual(filas, empresaId, usuarioId));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    @PostMapping("/repuesto/manual")
    public ResponseEntity<ImportBatchResultDTO> subirRepuestosManual(@RequestBody List<RepuestoImportRowDTO> filas) {

        Long empresaId = SecurityUtils.getEmpresaId();

        return ResponseEntity.ok(service.procesarRepuestosManual(filas, empresaId));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    @PostMapping("/ubicacion/manual")
    public ResponseEntity<ImportBatchResultDTO> subirUbicacionesManual(@RequestBody List<UbicacionImportRowDTO> filas) {

        Long empresaId = SecurityUtils.getEmpresaId();

        return ResponseEntity.ok(service.procesarUbicacionesManual(filas, empresaId));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_EMPRESA')")
    @PostMapping("/tipoActivo/manual")
    public ResponseEntity<ImportBatchResultDTO> subirTiposActivoManual(@RequestBody List<TipoActivoImportRowDTO> filas) {

        Long empresaId = SecurityUtils.getEmpresaId();

        return ResponseEntity.ok(service.procesarTiposActivoManual(filas, empresaId));
    }

}
