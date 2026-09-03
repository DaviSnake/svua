package cl.aracridav.svua.respaldo.controller;

import java.time.LocalDate;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.aracridav.svua.respaldo.service.RespaldoGeneralService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/svua/public/respaldo")
public class RespaldoController {

    private final RespaldoGeneralService respaldoGeneralService;

    // 🔒 Solo SUPER_ADMIN: es un respaldo de TODAS las empresas, no de
    // una empresa puntual (ver EmpresaController.backup para ese caso).
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/general")
    public ResponseEntity<byte[]> backupGeneral() {

        byte[] zip = respaldoGeneralService.generarBackup();
        String nombreArchivo = "respaldo_general_" + LocalDate.now() + ".zip";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .body(zip);
    }
}
