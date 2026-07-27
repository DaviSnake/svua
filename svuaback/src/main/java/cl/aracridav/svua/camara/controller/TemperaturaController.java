package cl.aracridav.svua.camara.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import cl.aracridav.svua.camara.entity.LecturaTemperatura;
import cl.aracridav.svua.camara.service.LecturaService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/svua/temperaturas")
@RequiredArgsConstructor
public class TemperaturaController {
    private final LecturaService lecturaService;

    @PostMapping("/lecturas")
    //@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA','TECNICO')")
    public ResponseEntity<Map<String, Object>> procesar(@RequestParam("imagen") MultipartFile imagen) {

        try {
                lecturaService.guardarImagen(imagen);
            } catch(Exception e) {
                e.printStackTrace();
            }

        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta(lecturaService.procesarYGuardar(imagen)));
    }

    @GetMapping("/lecturas")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA','TECNICO')")
    public ResponseEntity<List<Map<String, Object>>> listar() {
        return ResponseEntity.ok(lecturaService.listar().stream().map(this::respuesta).toList());
    }

    private Map<String, Object> respuesta(LecturaTemperatura lectura) {
        return Map.of("id", lectura.getId(), "temperatura", lectura.getTemperatura(),
                "textoOcr", lectura.getTextoOcr(), "fechaLectura", lectura.getFechaLectura());
    }
}
