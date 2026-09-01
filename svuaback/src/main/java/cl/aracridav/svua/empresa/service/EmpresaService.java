package cl.aracridav.svua.empresa.service;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import cl.aracridav.svua.auth.dto.response.AuthResponse;
import cl.aracridav.svua.empresa.dto.request.CreateEmpresaRequest;
import cl.aracridav.svua.empresa.dto.request.CreateEmpresaWithAdminRequest;
import cl.aracridav.svua.empresa.dto.request.UpdateEmpresaRequest;
import cl.aracridav.svua.empresa.dto.request.UpdatePlanEmpresaRequest;
import cl.aracridav.svua.empresa.dto.response.EmpresaResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface EmpresaService {

    public EmpresaResponse registrarEmpresa(CreateEmpresaRequest request);

    public EmpresaResponse actualizarEmpresa(Long empresaId, UpdateEmpresaRequest request);

    public EmpresaResponse actualizarPlan(Long empresaId, UpdatePlanEmpresaRequest request);

    public void eliminarEmpresa(Long empresaId);

    public EmpresaResponse registrarEmpresaConAdmin(CreateEmpresaWithAdminRequest request);

    public AuthResponse onboarding(CreateEmpresaWithAdminRequest request, HttpServletRequest httpRequest);

    public List<EmpresaResponse> obtenerEmpresa();

    public EmpresaResponse subirLogo(Long empresaId, MultipartFile archivo);

    public Resource obtenerLogo(Long empresaId);
}
