package cl.aracridav.svua.empresa.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

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
import cl.aracridav.svua.empresa.entity.TipoPlan;
import cl.aracridav.svua.empresa.repository.EmpresaRepository;
import cl.aracridav.svua.shared.enums.RolUsuario;
import cl.aracridav.svua.shared.exception.BusinessException;
import cl.aracridav.svua.shared.mappers.GeneralMapper;
import cl.aracridav.svua.shared.util.RutUtils;
import cl.aracridav.svua.shared.util.SecurityUtils;
import cl.aracridav.svua.usuario.entity.Usuario;
import cl.aracridav.svua.usuario.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class EmpresaServiceImpl implements EmpresaService {

    private static final Pattern COLOR_HEX_PATTERN = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final GeneralMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    /*
     * =========================================
     * REGISTRO
     * =========================================
     */

    @Override
    public EmpresaResponse registrarEmpresa(CreateEmpresaRequest request) {

        // 🔒 Si el RUT viene con puntos (ej: "12.345.678-9"), se guarda
        // sin puntos (ej: "12345678-9") para que la validación de RUT
        // único no dependa del formato ingresado.
        request.setRut(RutUtils.limpiarRut(request.getRut()));

        validarEmpresaUnica(request.getRut(), request.getNombre());

        Empresa empresa = construirEmpresaBase(request);
        aplicarConfiguracionPlan(empresa, request.getTipoPlan());

        empresaRepository.save(empresa);

        return mapper.mapEmpresaToResponse(empresa);
    }

    @Override
    @Transactional
    public EmpresaResponse registrarEmpresaConAdmin(CreateEmpresaWithAdminRequest request) {

        // 🔒 Si el RUT viene con puntos (ej: "12.345.678-9"), se guarda
        // sin puntos (ej: "12345678-9") para que la validación de RUT
        // único no dependa del formato ingresado.
        request.setRut(RutUtils.limpiarRut(request.getRut()));

        validarEmpresaUnica(request.getRut(), request.getNombre());
        validarEmailUnico(request.getAdminEmail());

        Empresa empresa = construirEmpresaBase(request);
        aplicarConfiguracionPlan(empresa, request.getTipoPlan());

        empresaRepository.save(empresa);

        crearAdminEmpresa(request, empresa);

        return mapper.mapEmpresaToResponse(empresa);
    }

    @Override
    @Transactional
    public AuthResponse onboarding(CreateEmpresaWithAdminRequest request, HttpServletRequest httpRequest) {

        // 🔒 Si el RUT viene con puntos (ej: "12.345.678-9"), se guarda
        // sin puntos (ej: "12345678-9") para que la validación de RUT
        // único no dependa del formato ingresado.
        request.setRut(RutUtils.limpiarRut(request.getRut()));

        validarEmpresaUnica(request.getRut(), request.getNombre());
        validarEmailUnico(request.getAdminEmail());

        Empresa empresa = construirEmpresaBase(request);
        aplicarConfiguracionPlan(empresa, request.getTipoPlan());

        empresaRepository.save(empresa);

        Usuario admin = crearAdminEmpresa(request, empresa);

        return generarAuthResponse(admin, empresa, httpRequest);
    }

    /*
     * =========================================
     * ACTUALIZACIÓN
     * =========================================
     */

    @Override
    public EmpresaResponse actualizarEmpresa(Long id, UpdateEmpresaRequest request) {

        Empresa empresa = obtenerEmpresa(id);

        actualizarCamposBasicos(empresa, request);

        if (request.getTipoPlan() != null) {
            aplicarConfiguracionPlan(empresa, request.getTipoPlan());
        }

        empresaRepository.save(empresa);

        return mapper.mapEmpresaToResponse(empresa);
    }

    @Override
    public EmpresaResponse actualizarPlan(Long id, UpdatePlanEmpresaRequest request) {

        Empresa empresa = obtenerEmpresa(id);

        aplicarConfiguracionPlan(empresa, request.getTipoPlan());

        empresa.setActiva(request.getActiva());
        empresa.setFechaInicioPlan(LocalDate.now());
        empresa.setFechaActualizacion(LocalDateTime.now());

        empresaRepository.save(empresa);

        return mapper.mapEmpresaToResponse(empresa);
    }

    /*
     * =========================================
     * ELIMINACIÓN
     * =========================================
     */

    @Override
    @Transactional
    public void eliminarEmpresa(Long id) {

        Empresa empresa = obtenerEmpresa(id);

        empresa.setActiva(false);
        empresa.setFechaFinPlan(LocalDate.now());

        empresaRepository.save(empresa);
    }

    /*
     * =========================================
     * CONSULTA
     * =========================================
     */

    @Override
    public List<EmpresaResponse> obtenerEmpresa() {

        Long empresaId = SecurityUtils.getEmpresaId();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean esSuperAdmin = tieneRol(auth, "ROLE_SUPER_ADMIN");
        boolean esAdminEmpresa = tieneRol(auth, "ROLE_ADMIN_EMPRESA", "EMPRESA_VIEW");

        List<Empresa> empresas = esSuperAdmin
                ? empresaRepository.findAll()
                : esAdminEmpresa
                    ? empresaRepository.findById(empresaId).map(List::of).orElse(List.of())
                    : List.of();

        return empresas.stream()
                .map(mapper::mapEmpresaToResponse)
                .toList();
    }

    /*
     * =========================================
     * PLAN (🔥 CLAVE DEL REFACTOR)
     * =========================================
     */

    private void aplicarConfiguracionPlan(Empresa empresa, TipoPlan plan) {

        empresa.setTipoPlan(plan);
        empresa.setFechaInicioPlan(LocalDate.now());
        empresa.setFechaActualizacion(LocalDateTime.now());
        empresa.setActiva(true);

        switch (plan) {
            case FREE -> configurarPlan(empresa, 2, 20, LocalDate.now().plusDays(15));
            case BASICO -> configurarPlan(empresa, 5, 100, LocalDate.now().plusMonths(1));
            case PROFESIONAL -> configurarPlan(empresa, 10, 150, LocalDate.now().plusMonths(6));
            case ENTERPRISE -> configurarPlan(empresa, 50, 1000, LocalDate.now().plusYears(1));
            default -> throw new BusinessException("Plan no válido");
        }
    }

    private void configurarPlan(Empresa empresa, int maxUsuarios, int maxActivos, LocalDate fechaFin) {
        empresa.setMaxUsuarios(maxUsuarios);
        empresa.setMaxActivos(maxActivos);
        empresa.setFechaFinPlan(fechaFin);
    }

    /*
     * =========================================
     * CREACIÓN
     * =========================================
     */

    private Empresa construirEmpresaBase(CreateEmpresaRequest request) {

        Empresa empresa = new Empresa();

        empresa.setNombre(request.getNombre());
        empresa.setRut(request.getRut());
        empresa.setEmailContacto(request.getEmailContacto());
        empresa.setTelefono(request.getTelefono());
        empresa.setDireccion(request.getDireccion());
        empresa.setFechaCreacion(LocalDateTime.now());
        empresa.setDemo(Boolean.TRUE.equals(request.getDemo()));
        empresa.setCodigoQrHabilitado(Boolean.TRUE.equals(request.getCodigoQrHabilitado()));
        empresa.setCodigoEan13Habilitado(Boolean.TRUE.equals(request.getCodigoEan13Habilitado()));
        empresa.setControlTurnoHabilitado(Boolean.TRUE.equals(request.getControlTurnoHabilitado()));
        empresa.setHojaControlHabilitado(Boolean.TRUE.equals(request.getHojaControlHabilitado()));
        empresa.setInformeMantencionesHabilitado(Boolean.TRUE.equals(request.getInformeMantencionesHabilitado()));

        return empresa;
    }

    private Usuario crearAdminEmpresa(CreateEmpresaWithAdminRequest request, Empresa empresa) {

        Usuario admin = new Usuario();

        admin.setNombre(request.getAdminNombre());
        admin.setEmail(request.getAdminEmail());
        admin.setPassword(passwordEncoder.encode(request.getAdminPassword()));
        admin.setRol(RolUsuario.ADMIN_EMPRESA);
        admin.setActivo(true);
        admin.setEmpresa(empresa);

        return usuarioRepository.save(admin);
    }

    private AuthResponse generarAuthResponse(Usuario admin, Empresa empresa, HttpServletRequest request) {

        UsuarioPrincipal principal = new UsuarioPrincipal(admin);

        String device = request.getHeader("User-Agent");
        String ip = request.getRemoteAddr();

        // 🐛 FIX: antes se llamaba jwtService.generateToken(principal) (el
        // overload de 1 argumento), que firma con una clave distinta a la
        // que usa el validador (getClaims()) y ademas no agrega los claims
        // "userName"/"empresaId"/"jti" que JwtAuthenticationFilter necesita
        // -- el access token que devolvía el onboarding quedaba "roto": la
        // primera petición del usuario recién creado fallaba (firma
        // inválida / usuario no autenticado). Se usa el mismo overload que
        // login()/refreshToken(), con un tokenJti nuevo.
        String tokenJti = UUID.randomUUID().toString();
        String accessToken = jwtService.generateToken(principal, tokenJti);
        String refreshToken = refreshTokenService
                .createRefreshToken(admin, empresa, device, ip)
                .getToken();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .email(admin.getEmail())
                .rol(admin.getRol())
                .empresaId(empresa.getId())
                .build();
    }

    /*
     * =========================================
     * VALIDACIONES
     * =========================================
     */

    private void validarEmpresaUnica(String rut, String nombre) {
        if (empresaRepository.existsByRut(rut)) {
            throw new BusinessException("El RUT ya está registrado");
        }
        if (empresaRepository.existsByNombre(nombre)) {
            throw new BusinessException("Ya existe una empresa con ese nombre");
        }
    }

    private void validarEmailUnico(String email) {
        if (usuarioRepository.existsByEmail(email)) {
            throw new BusinessException("El email ya está registrado");
        }
    }

    /*
     * =========================================
     * HELPERS
     * =========================================
     */

    private Empresa obtenerEmpresa(Long id) {
        Empresa empresa = empresaRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Empresa no encontrada"));

        // 🔐 Validación multi-tenant
        if (!SecurityUtils.esSuperAdmin() && !empresa.getId().equals(SecurityUtils.getEmpresaId())) {
            throw new BusinessException("No pertenece a esta empresa");
        }

        return empresa;
    }

    private boolean tieneRol(Authentication auth, String... roles) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> Arrays.asList(roles).contains(a.getAuthority()));
    }

    private void actualizarCamposBasicos(Empresa empresa, UpdateEmpresaRequest request) {

        if (request.getNombre() != null) empresa.setNombre(request.getNombre());
        if (request.getRut() != null) empresa.setRut(RutUtils.limpiarRut(request.getRut()));
        if (request.getDireccion() != null) empresa.setDireccion(request.getDireccion());
        if (request.getTelefono() != null) empresa.setTelefono(request.getTelefono());
        if (request.getActiva() != null) empresa.setActiva(request.getActiva());
        if (request.getDemo() != null) empresa.setDemo(request.getDemo());
        if (request.getCodigoQrHabilitado() != null) empresa.setCodigoQrHabilitado(request.getCodigoQrHabilitado());
        if (request.getCodigoEan13Habilitado() != null) empresa.setCodigoEan13Habilitado(request.getCodigoEan13Habilitado());
        if (request.getControlTurnoHabilitado() != null) empresa.setControlTurnoHabilitado(request.getControlTurnoHabilitado());
        if (request.getHojaControlHabilitado() != null) empresa.setHojaControlHabilitado(request.getHojaControlHabilitado());
        if (request.getInformeMantencionesHabilitado() != null) empresa.setInformeMantencionesHabilitado(request.getInformeMantencionesHabilitado());

        if (request.getColorPrimario() != null) {
            validarColorPrimario(request.getColorPrimario());
            empresa.setColorPrimario(request.getColorPrimario());
        }
    }

    // 🔒 Se valida el formato porque este valor termina inyectado tal
    // cual en una variable CSS en el frontend (sidebar.component.ts) --
    // sin esto, cualquier texto llegaría directo al DOM.
    private void validarColorPrimario(String colorPrimario) {
        if (!COLOR_HEX_PATTERN.matcher(colorPrimario).matches()) {
            throw new BusinessException("El color primario debe ser un hexadecimal válido (ej. #3498db)");
        }
    }

    /*
     * =========================================
     * LOGO (personalizacion por empresa)
     * =========================================
     */

    @Override
    public EmpresaResponse subirLogo(Long empresaId, MultipartFile archivo) {

        if (archivo == null || archivo.isEmpty()) {
            throw new BusinessException("Debe adjuntar un archivo");
        }

        if (archivo.getSize() > 2 * 1024 * 1024) {
            throw new BusinessException("El logo no puede superar 2MB");
        }

        String contentType = archivo.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("El logo debe ser una imagen");
        }

        Empresa empresa = obtenerEmpresa(empresaId);

        String rutaAnterior = empresa.getLogoRutaArchivo();

        String ruta = guardarLogoSeguro(archivo, empresa);
        empresa.setLogoRutaArchivo(ruta);

        Empresa guardada = empresaRepository.save(empresa);

        // 🔥 Se borra el archivo viejo DESPUES de guardar la referencia
        // nueva en BD: si el guardado fallara, no queremos habernos
        // quedado sin logo antiguo Y sin el nuevo.
        if (rutaAnterior != null && !rutaAnterior.equals(ruta)) {
            try {
                Files.deleteIfExists(Paths.get(rutaAnterior));
            } catch (IOException ignored) {
                // No es crítico: el archivo viejo queda huérfano en disco.
            }
        }

        return mapper.mapEmpresaToResponse(guardada);
    }

    // 🔒 Sin validación multi-tenant a propósito: este método lo llama
    // el endpoint público GET /public/empresas/{id}/logo (sin
    // autenticación, para que un <img src> funcione directo), a
    // diferencia de obtenerEmpresa() que exige empresaId == empresa del
    // usuario logueado.
    @Override
    @Transactional(readOnly = true)
    public Resource obtenerLogo(Long empresaId) {

        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new BusinessException("Empresa no encontrada"));

        if (empresa.getLogoRutaArchivo() == null || empresa.getLogoRutaArchivo().isBlank()) {
            throw new BusinessException("Esta empresa no tiene un logo cargado");
        }

        try {
            Path path = Paths.get(empresa.getLogoRutaArchivo());

            if (!Files.exists(path)) {
                throw new BusinessException("Archivo no encontrado");
            }

            return new UrlResource(path.toUri());

        } catch (IOException e) {
            throw new BusinessException("Error al leer el logo");
        }
    }

    private String guardarLogoSeguro(MultipartFile archivo, Empresa empresa) {

        try {
            Path carpeta = Paths.get("uploads/logos");

            if (!Files.exists(carpeta)) {
                Files.createDirectories(carpeta);
            }

            String nombreLimpio = StringUtils.cleanPath(
                archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "logo");

            String extension = nombreLimpio.contains(".")
                ? nombreLimpio.substring(nombreLimpio.lastIndexOf('.'))
                : "";

            // 🔥 Nombre fijo por empresa (no se le agrega timestamp): cada
            // subida reemplaza el logo anterior, así que solo debe existir
            // un archivo por empresa en esta carpeta.
            String nombreArchivo = empresa.getId() + extension;

            Path ruta = carpeta.resolve(nombreArchivo);

            Files.copy(archivo.getInputStream(), ruta, StandardCopyOption.REPLACE_EXISTING);

            return ruta.toString();

        } catch (IOException e) {
            throw new BusinessException("Error al guardar el logo");
        }
    }
}