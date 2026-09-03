package cl.aracridav.svua.empresa.controller;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import cl.aracridav.svua.auth.dto.response.AuthResponse;
import cl.aracridav.svua.empresa.dto.request.CreateEmpresaRequest;
import cl.aracridav.svua.empresa.dto.request.CreateEmpresaWithAdminRequest;
import cl.aracridav.svua.empresa.dto.request.UpdateEmpresaRequest;
import cl.aracridav.svua.empresa.dto.request.UpdatePlanEmpresaRequest;
import cl.aracridav.svua.empresa.dto.response.EmpresaResponse;
import cl.aracridav.svua.empresa.service.EmpresaBackupService;
import cl.aracridav.svua.empresa.service.EmpresaService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/v1/svua/public/empresas")
public class EmpresaController {

    private final EmpresaService empresaService;
    private final EmpresaBackupService empresaBackupService;

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA')")
    @PostMapping
    public ResponseEntity<EmpresaResponse> crear(
            @RequestBody CreateEmpresaRequest request) {

        EmpresaResponse response =
                empresaService.registrarEmpresa(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA','TECNICO') or " +
        "(hasAuthority('EMPRESA_VIEW'))"
    )
    @GetMapping
    public ResponseEntity<List<EmpresaResponse>> obtener() {
        List<EmpresaResponse> response =
                empresaService.obtenerEmpresa();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA') or " +
        "(hasAuthority('EMPRESA_UPDATE') and " +
        "#empresaId == authentication.principal.empresaId)"
    )
    @PutMapping("/{empresaId}")
    public ResponseEntity<EmpresaResponse> actualizar(
            @PathVariable Long empresaId,
            @RequestBody UpdateEmpresaRequest request) {

        EmpresaResponse response =
                empresaService.actualizarEmpresa(empresaId, request);

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA')")
    @PutMapping("/{empresaId}/plan")
    public ResponseEntity<EmpresaResponse> actualizarPlan(
            @PathVariable Long empresaId,
            @RequestBody UpdatePlanEmpresaRequest request) {

        return ResponseEntity.ok(
                empresaService.actualizarPlan(empresaId, request));
    }

    @PreAuthorize("hasAuthority('EMPRESA_DELETE')")
    @DeleteMapping("/{empresaId}")
    public ResponseEntity<Void> eliminarEmpresa(@PathVariable Long empresaId) {

        empresaService.eliminarEmpresa(empresaId);

        return ResponseEntity.noContent().build();
    }

    // 🔥 Personalización: logo propio de la empresa. Solo SUPER_ADMIN
    // (cualquier empresa) o ADMIN_EMPRESA de la propia empresa (misma
    // validación multi-tenant que actualizar(), ver
    // EmpresaServiceImpl.obtenerEmpresa).
    @PreAuthorize(
        "hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA') or " +
        "(hasAuthority('EMPRESA_UPDATE') and " +
        "#empresaId == authentication.principal.empresaId)"
    )
    @PostMapping("/{empresaId}/logo")
    public ResponseEntity<EmpresaResponse> subirLogo(
            @PathVariable Long empresaId,
            @RequestParam("file") MultipartFile file) {

        return ResponseEntity.ok(empresaService.subirLogo(empresaId, file));
    }

    // 🔥 Sin @PreAuthorize a propósito: esta ruta cuelga de
    // /public/empresas (permitAll en SecurityConfig) para que un
    // <img src="..."> del sidebar la pueda cargar directo, sin poder
    // adjuntar un header Authorization. Es solo una imagen de logo, no
    // hay dato sensible que proteger.
    @GetMapping("/{empresaId}/logo")
    public ResponseEntity<Resource> obtenerLogo(@PathVariable Long empresaId) {

        Resource logo = empresaService.obtenerLogo(empresaId);

        String contentType = java.net.URLConnection.guessContentTypeFromName(logo.getFilename());

        return ResponseEntity.ok()
                .contentType(contentType != null
                    ? MediaType.parseMediaType(contentType)
                    : MediaType.APPLICATION_OCTET_STREAM)
                .body(logo);
    }

    // 🔒 Respaldo puntual de una sola empresa (ver EmpresaBackupService):
    // solo SUPER_ADMIN, ninguna empresa deberia poder descargar sus
    // propios datos "en crudo" por esta via.
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/{empresaId}/backup")
    public ResponseEntity<byte[]> backup(@PathVariable Long empresaId) {

        byte[] zip = empresaBackupService.generarBackup(empresaId);
        String nombreArchivo = "empresa_" + empresaId + "_backup_"
                + java.time.LocalDate.now() + ".zip";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"" + nombreArchivo + "\"")
                .body(zip);
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA')")
    @PostMapping("/onboarding")
    public ResponseEntity<EmpresaResponse> crearConAdmin(
            @RequestBody CreateEmpresaWithAdminRequest request) {

        EmpresaResponse response =
                empresaService.registrarEmpresaConAdmin(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_EMPRESA')")
    @PostMapping("/onboardinglogueado")
    public ResponseEntity<AuthResponse> onboarding(
            @RequestBody CreateEmpresaWithAdminRequest request, HttpServletRequest httpRequest) {

        AuthResponse response =
                empresaService.onboarding(request, httpRequest);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    
}
