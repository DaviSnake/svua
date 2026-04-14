package cl.aracridav.svua.empresa.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.aracridav.svua.auth.dto.response.AuthResponse;
import cl.aracridav.svua.empresa.dto.request.CreateEmpresaRequest;
import cl.aracridav.svua.empresa.dto.request.CreateEmpresaWithAdminRequest;
import cl.aracridav.svua.empresa.dto.request.UpdateEmpresaRequest;
import cl.aracridav.svua.empresa.dto.request.UpdatePlanEmpresaRequest;
import cl.aracridav.svua.empresa.dto.response.EmpresaResponse;
import cl.aracridav.svua.empresa.service.EmpresaService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/v1/svua/public/empresas")
public class EmpresaController {

    private final EmpresaService empresaService;

    @PreAuthorize("hasRole('SUPER_ADMIN')")
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
        "hasRole('SUPER_ADMIN') or " +
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
        "hasRole('SUPER_ADMIN') or " +
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

    @PreAuthorize("hasRole('SUPER_ADMIN')")
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

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/onboarding")
    public ResponseEntity<EmpresaResponse> crearConAdmin(
            @RequestBody CreateEmpresaWithAdminRequest request) {

        EmpresaResponse response =
                empresaService.registrarEmpresaConAdmin(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
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
