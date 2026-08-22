package cl.aracridav.svua.shared.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.aracridav.svua.shared.dto.response.LogArchivoResponse;
import cl.aracridav.svua.shared.service.LogArchivoService;
import lombok.RequiredArgsConstructor;

// 🔥 Pantalla "Ver logs" — solo SUPER_ADMIN. Lista y muestra el contenido
// de los .txt de error generados por las cargas masivas (ver
// ImportFileLogService), guardados en log/{empresaId}/.
@RestController
@RequestMapping("/api/v1/svua/logs")
@RequiredArgsConstructor
public class LogArchivoController {

    private final LogArchivoService logArchivoService;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Page<LogArchivoResponse> listar(
            @RequestParam(required = false) Long empresaId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);

        return logArchivoService.listar(empresaId, pageable);
    }

    @GetMapping(value = "/archivo", produces = MediaType.TEXT_PLAIN_VALUE)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<String> verArchivo(
            @RequestParam Long empresaId,
            @RequestParam String nombreArchivo) {

        return ResponseEntity.ok(
            logArchivoService.leerContenido(empresaId, nombreArchivo));
    }
}
