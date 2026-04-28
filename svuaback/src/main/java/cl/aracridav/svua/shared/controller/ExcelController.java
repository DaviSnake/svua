package cl.aracridav.svua.shared.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

}
