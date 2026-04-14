package cl.aracridav.svua.empresa.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.aracridav.svua.auth.dto.response.AuthResponse;
import cl.aracridav.svua.auth.service.RefreshTokenService;
import cl.aracridav.svua.config.security.JwtService;
import cl.aracridav.svua.config.security.UsuarioPrincipal;
import cl.aracridav.svua.empresa.dto.request.CreateEmpresaRequest;
import cl.aracridav.svua.empresa.dto.request.CreateEmpresaWithAdminRequest;
import cl.aracridav.svua.empresa.dto.request.UpdateEmpresaRequest;
import cl.aracridav.svua.empresa.dto.request.UpdatePlanEmpresaRequest;
import cl.aracridav.svua.empresa.dto.response.EmpresaResponse;
import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.empresa.repository.EmpresaRepository;
import cl.aracridav.svua.shared.enums.RolUsuario;
import cl.aracridav.svua.shared.exception.BusinessException;
import cl.aracridav.svua.shared.mappers.GeneralMapper;
import cl.aracridav.svua.shared.util.SecurityUtils;
import cl.aracridav.svua.usuario.entity.Usuario;
import cl.aracridav.svua.usuario.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class EmpresaServiceImpl implements EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final GeneralMapper generalMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Override
    public EmpresaResponse registrarEmpresa(
        CreateEmpresaRequest request) {

        if (empresaRepository.existsByRut(request.getRut())) {
            throw new BusinessException("El RUT ya está registrado");
        }

        if (empresaRepository.existsByNombre(request.getNombre())) {
            throw new BusinessException("Ya existe una empresa con ese nombre");
        }

        Empresa empresa = new Empresa();

        empresa.setNombre(request.getNombre());
        empresa.setRut(request.getRut());
        empresa.setEmailContacto(request.getEmailContacto());
        empresa.setTelefono(request.getTelefono());
        empresa.setDireccion(request.getDireccion());

        // 🔹 Configuración SaaS
        empresa.setTipoPlan(request.getTipoPlan());
        
        // 🔥 Lógica automática por plan
        switch (request.getTipoPlan()) {

            case FREE -> {
                empresa.setMaxUsuarios(2);
                empresa.setMaxActivos(20);
                empresa.setFechaFinPlan(LocalDate.now().plusDays(15));
            }

            case BASICO -> {
                empresa.setMaxUsuarios(5);
                empresa.setMaxActivos(100);
                empresa.setFechaFinPlan(LocalDate.now().plusMonths(1));
            }

            case PROFESIONAL -> {
                empresa.setMaxUsuarios(10);
                empresa.setMaxActivos(150);
                empresa.setFechaFinPlan(LocalDate.now().plusMonths(6));
            }

            case ENTERPRISE -> {
                empresa.setMaxUsuarios(50);
                empresa.setMaxActivos(1000);
                empresa.setFechaFinPlan(LocalDate.now().plusYears(1));
            }

            default -> throw new BusinessException("Plan no válido");
        }

        empresa.setFechaInicioPlan(LocalDate.now());
        empresa.setFechaActualizacion(LocalDateTime.now());

        empresa.setActiva(true);

        // 🔹 Auditoría
        empresa.setFechaCreacion(LocalDateTime.now());

        empresaRepository.save(empresa);

        return generalMapper.mapEmpresaToResponse(empresa);
    }

    @Override
    public EmpresaResponse actualizarEmpresa(
        Long empresaId,
        UpdateEmpresaRequest request) {

        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() ->
                        new BusinessException("Empresa no encontrada"));

        // 🔐 Actualización parcial segura
        if (request.getNombre() != null) {
            empresa.setNombre(request.getNombre());
        }

        if (request.getRut() != null) {
            empresa.setRut(request.getRut());
        }

        if (request.getDireccion() != null) {
            empresa.setDireccion(request.getDireccion());
        }

        if (request.getTelefono() != null) {
            empresa.setTelefono(request.getTelefono());
        }

        if (request.getActiva() != null) {
            empresa.setActiva(request.getActiva());
        }

        empresaRepository.save(empresa);

        return EmpresaResponse.builder()
                .id(empresa.getId())
                .nombre(empresa.getNombre())
                .rut(empresa.getRut())
                .direccion(empresa.getDireccion())
                .telefono(empresa.getTelefono())
                .activa(empresa.getActiva())
                .build();
    }

    @Override
    public EmpresaResponse actualizarPlan(
        Long empresaId,
        UpdatePlanEmpresaRequest request) {

        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() ->
                        new BusinessException("Empresa no encontrada"));

        // 🔹 Configuración SaaS
        empresa.setTipoPlan(request.getTipoPlan());

        // 🔥 Lógica automática por plan
        switch (request.getTipoPlan()) {

            case FREE -> {
                empresa.setMaxUsuarios(2);
                empresa.setMaxActivos(20);
                empresa.setFechaFinPlan(LocalDate.now().plusDays(15));
            }

            case BASICO -> {
                empresa.setMaxUsuarios(5);
                empresa.setMaxActivos(100);
                empresa.setFechaFinPlan(LocalDate.now().plusMonths(1));
            }

            case PROFESIONAL -> {
                empresa.setMaxUsuarios(10);
                empresa.setMaxActivos(150);
                empresa.setFechaFinPlan(LocalDate.now().plusMonths(6));
            }

            case ENTERPRISE -> {
                empresa.setMaxUsuarios(50);
                empresa.setMaxActivos(1000);
                empresa.setFechaFinPlan(LocalDate.now().plusYears(1));
            }

            default -> throw new BusinessException("Plan no válido");
        }
        
        
   
        empresa.setActiva(request.getActiva());

        empresa.setFechaInicioPlan(LocalDate.now());
        empresa.setFechaActualizacion(LocalDateTime.now());

        empresaRepository.save(empresa);

        return generalMapper.mapEmpresaToResponse(empresa);
    }

    @Override
    @Transactional
    public void eliminarEmpresa(Long empresaId) {

        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new BusinessException("Empresa no encontrada"));

        empresa.setActiva(false);
        empresa.setFechaFinPlan(LocalDate.now());

        empresaRepository.save(empresa);
    }

    @Transactional
    public EmpresaResponse registrarEmpresaConAdmin(
            CreateEmpresaWithAdminRequest request) {

        if (empresaRepository.existsByRut(request.getRut())) {
            throw new BusinessException("El RUT ya está registrado");
        }

        if (usuarioRepository.existsByEmail(request.getAdminEmail())) {
            throw new BusinessException("El email del admin ya está registrado");
        }

        // 🔹 Crear empresa
        Empresa empresa = new Empresa();

        empresa.setNombre(request.getNombre());
        empresa.setRut(request.getRut());
        empresa.setEmailContacto(request.getEmailContacto());
        empresa.setTelefono(request.getTelefono());
        empresa.setDireccion(request.getDireccion());

        empresa.setTipoPlan(request.getTipoPlan());
        empresa.setFechaInicioPlan(LocalDate.now());

        // 🔥 Lógica automática por plan
        switch (request.getTipoPlan()) {

            case FREE -> {
                empresa.setMaxUsuarios(2);
                empresa.setMaxActivos(20);
                empresa.setFechaFinPlan(LocalDate.now().plusDays(15));
            }

            case BASICO -> {
                empresa.setMaxUsuarios(5);
                empresa.setMaxActivos(100);
                empresa.setFechaFinPlan(LocalDate.now().plusMonths(1));
            }

            case PROFESIONAL -> {
                empresa.setMaxUsuarios(10);
                empresa.setMaxActivos(150);
                empresa.setFechaFinPlan(LocalDate.now().plusMonths(6));
            }

            case ENTERPRISE -> {
                empresa.setMaxUsuarios(50);
                empresa.setMaxActivos(1000);
                empresa.setFechaFinPlan(LocalDate.now().plusYears(1));
            }

            default -> throw new BusinessException("Plan no válido");
        }

        empresa.setActiva(true);
        empresa.setFechaCreacion(LocalDateTime.now());
        empresa.setFechaActualizacion(LocalDateTime.now());

        empresaRepository.save(empresa);

        // 🔹 Crear ADMIN_EMPRESA automáticamente
        Usuario admin = new Usuario();
            admin.setNombre(request.getAdminNombre());
            admin.setEmail(request.getAdminEmail());
            admin.setPassword(passwordEncoder.encode(request.getAdminPassword()));
            admin.setRol(RolUsuario.ADMIN_EMPRESA);
            admin.setActivo(true);
            admin.setEmpresa(empresa);

        usuarioRepository.save(admin);

        return generalMapper.mapEmpresaToResponse(empresa);
    }

    @Transactional
    public AuthResponse onboarding(CreateEmpresaWithAdminRequest request, HttpServletRequest httpRequest) {

        if (empresaRepository.existsByRut(request.getRut())) {
            throw new BusinessException("El RUT ya está registrado");
        }

        if (usuarioRepository.existsByEmail(request.getAdminEmail())) {
            throw new BusinessException("El email ya está registrado");
        }

        // 🔹 Crear Empresa
        Empresa empresa = new Empresa();

        empresa.setNombre(request.getNombre());
        empresa.setRut(request.getRut());
        empresa.setEmailContacto(request.getEmailContacto());
        empresa.setTelefono(request.getTelefono());
        empresa.setDireccion(request.getDireccion());

        empresa.setTipoPlan(request.getTipoPlan());
        empresa.setFechaInicioPlan(LocalDate.now());

        switch (request.getTipoPlan()) {

            case FREE -> {
                empresa.setMaxUsuarios(2);
                empresa.setMaxActivos(20);
                empresa.setFechaFinPlan(LocalDate.now().plusDays(15));
            }

            case BASICO -> {
                empresa.setMaxUsuarios(5);
                empresa.setMaxActivos(100);
                empresa.setFechaFinPlan(LocalDate.now().plusMonths(1));
            }

            case PROFESIONAL -> {
                empresa.setMaxUsuarios(10);
                empresa.setMaxActivos(150);
                empresa.setFechaFinPlan(LocalDate.now().plusMonths(6));
            }

            case ENTERPRISE -> {
                empresa.setMaxUsuarios(50);
                empresa.setMaxActivos(1000);
                empresa.setFechaFinPlan(LocalDate.now().plusYears(1));
            }

            default -> throw new BusinessException("Plan no válido");
        }

        empresa.setActiva(true);
        empresa.setFechaCreacion(LocalDateTime.now());
        empresa.setFechaActualizacion(LocalDateTime.now());

        empresaRepository.save(empresa);

        // 🔹 Crear ADMIN_EMPRESA
        Usuario admin = new Usuario();
            admin.setNombre(request.getAdminNombre());
            admin.setEmail(request.getAdminEmail());
            admin.setPassword(passwordEncoder.encode(request.getAdminPassword()));
            admin.setRol(RolUsuario.ADMIN_EMPRESA);
            admin.setActivo(true);
            admin.setEmpresa(empresa);

        usuarioRepository.save(admin);

        // 🔹 Generar UsuarioPrincipal
        UsuarioPrincipal principal = new UsuarioPrincipal(admin);

        // Datos del dispositivo
        String device = httpRequest.getHeader("User-Agent");
        String ip = httpRequest.getRemoteAddr();

        // 🔹 Generar JWT
        String accessToken = jwtService.generateToken(principal);
        String refreshToken = refreshTokenService.createRefreshToken(admin, empresa, device, ip).getToken();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .email(admin.getEmail())
                .rol(admin.getRol())
                .empresaId(empresa.getId())
                .build();
    }

    public List<EmpresaResponse> obtenerEmpresa(){

        List<Empresa> listaEmpresas = new ArrayList<>();

        Long empresaId = SecurityUtils.getEmpresaId();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean esSuperAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        
        boolean esAdminEmpresa = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN_EMPRESA")
                            || a.getAuthority().equals("EMPRESA_VIEW"));

        if (esSuperAdmin) {
            listaEmpresas = empresaRepository.findAll();
        } else if (esAdminEmpresa) {
            listaEmpresas = empresaRepository.findById(empresaId)
            .map(List::of)
            .orElse(List.of());
        }

        List<EmpresaResponse> response = listaEmpresas.stream()
        .map(generalMapper::mapEmpresaToResponse)
        .toList();

        return response;
    }
}
