package cl.aracridav.svua.usuario.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.aracridav.svua.auth.dto.request.ChangePasswordRequest;
import cl.aracridav.svua.config.security.SecurityService;
import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.empresa.repository.EmpresaRepository;
import cl.aracridav.svua.shared.enums.RolUsuario;
import cl.aracridav.svua.shared.exception.BusinessException;
import cl.aracridav.svua.shared.mappers.GeneralMapper;
import cl.aracridav.svua.shared.util.SecurityUtils;
import cl.aracridav.svua.usuario.dto.request.RegisterRequest;
import cl.aracridav.svua.usuario.dto.request.UpdateUsuarioRequest;
import cl.aracridav.svua.usuario.dto.response.PerfilUsuarioDTO;
import cl.aracridav.svua.usuario.dto.response.UsuarioResponse;
import cl.aracridav.svua.usuario.entity.PasswordResetToken;
import cl.aracridav.svua.usuario.entity.Usuario;
import cl.aracridav.svua.usuario.repository.PasswordResetTokenRepository;
import cl.aracridav.svua.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final PasswordResetTokenRepository tokenRepo;
    private final PasswordEncoder passwordEncoder;
    private final GeneralMapper generalMapper;
    private final SecurityService securityService;

    // ===============================
    // CREATE
    // ===============================
    @Override
    public UsuarioResponse registrarUsuario(RegisterRequest request) {

        Long empresaId = resolveEmpresaId(request.getEmpresaId());

        validarEmailUnico(request.getEmail());
        validarRolCreacion(request.getRol());

        Empresa empresa = obtenerEmpresa(empresaId);

        Usuario usuario = construirUsuario(request, empresa);

        return generalMapper.mapUsuarioToResponse(
                usuarioRepository.save(usuario)
        );
    }

    // ===============================
    // UPDATE
    // ===============================
    @Override
    public UsuarioResponse actualizarUsuario(Long usuarioId, UpdateUsuarioRequest request) {

        Usuario usuario = obtenerUsuario(usuarioId);

        validarAccesoEmpresa(usuario);
        validarNoSuperAdmin(usuario);
        validarEscalamientoRol(request.getRol());

        aplicarCambios(usuario, request);

        return generalMapper.mapUsuarioToResponse(
                usuarioRepository.save(usuario)
        );
    }

    // ===============================
    // DELETE (soft)
    // ===============================
    @Override
    public void eliminarUsuario(Long usuarioId) {

        Usuario usuario = obtenerUsuario(usuarioId);

        validarAccesoEmpresa(usuario);
        validarNoSuperAdmin(usuario);
        validarNoAutoEliminacion(usuarioId);

        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }

    // ===============================
    // PASSWORD
    // ===============================
    @Override
    public void cambiarPassword(Long usuarioId, ChangePasswordRequest request) {

        Usuario usuario = obtenerUsuario(usuarioId);

        validarCambioPassword(usuarioId);
        validarEmpresa(usuario);
        validarPasswordActual(request, usuario);
        validarNuevaPassword(request.getNewPassword());

        usuario.setPassword(
                passwordEncoder.encode(request.getNewPassword())
        );

        usuarioRepository.save(usuario);
    }

    // ===============================
    // CREAR TOKEN
    // ===============================
    @Override
    public String createToken(Usuario user) {

        tokenRepo.deleteByUser(user);

        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(30));
        resetToken.setEmpresa(user.getEmpresa());

        tokenRepo.save(resetToken);

        return token;
    }

    // ===============================
    // VALIDAR TOKEN
    // ===============================
    @Override
    public PasswordResetToken validateToken(String token) {

        PasswordResetToken t = tokenRepo.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token inválido"));

        if (t.isUsed()) {
            throw new RuntimeException("Token ya utilizado");
        }

        if (t.getExpiryDate().isBefore(LocalDateTime.now())) {
            tokenRepo.delete(t);
            throw new RuntimeException("Token expirado");
        }

        return t;
    }

    // ===============================
    // RESETEAR PASSWORD
    // ===============================
    @Override
    public void resetPassword(String token, String newPassword) {

        PasswordResetToken t = validateToken(token);

        Usuario user = t.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));

        usuarioRepository.save(user);

        // 🔥 invalidar token
        t.setUsed(true);
        tokenRepo.save(t);
    }

    // ===============================
    // LIST
    // ===============================
    @Override
    public Page<UsuarioResponse> listarUsuarios(Pageable pageable, Long empresaId) {

        Long empresaPropia = SecurityUtils.getEmpresaId();

        if (esSuperAdmin()) {
            // 🔥 SUPER_ADMIN puede ver todas las empresas o filtrar por una
            if (empresaId != null) {
                return usuarioRepository.findByEmpresaId(empresaId, pageable)
                        .map(generalMapper::mapUsuarioToResponse);
            }
            return usuarioRepository.findAll(pageable)
                    .map(generalMapper::mapUsuarioToResponse);
        }

        if (esAdminEmpresa()) {
            // 🔒 ADMIN_EMPRESA siempre ve solo su propia empresa, sin
            // importar lo que llegue en empresaId.
            return usuarioRepository.findByEmpresaId(empresaPropia, pageable)
                    .map(generalMapper::mapUsuarioToResponse);
        }

        return new PageImpl<>(List.of(securityService.obtenerUsuarioAutenticado()))
                .map(generalMapper::mapUsuarioToResponse);
    }

    // ===============================
    // PERFIL
    // ===============================
    @Override
    public PerfilUsuarioDTO perfilUsuario() {
        return generalMapper.mapUsuariotoPerfilDTO(
                securityService.obtenerUsuarioAutenticado()
        );
    }

    // ===============================
    // 🔒 VALIDACIONES
    // ===============================

    private void validarEmailUnico(String email) {
        if (usuarioRepository.existsByEmail(email)) {
            throw new BusinessException("El email ya está registrado");
        }
    }

    private void validarRolCreacion(RolUsuario rol) {
        if (rol == RolUsuario.SUPER_ADMIN) {
            throw new BusinessException("No se permite crear usuarios SUPER ADMIN");
        }
    }

    private void validarNoSuperAdmin(Usuario usuario) {
        if (usuario.getRol() == RolUsuario.SUPER_ADMIN) {
            throw new BusinessException("No se puede modificar/eliminar un SUPER ADMIN");
        }
    }

    private void validarEscalamientoRol(RolUsuario nuevoRol) {
        if (!esSuperAdmin() && nuevoRol == RolUsuario.SUPER_ADMIN) {
            throw new BusinessException("No puedes asignar rol SUPER_ADMIN");
        }
    }

    private void validarAccesoEmpresa(Usuario usuario) {
        if (!esSuperAdmin() &&
            !usuario.getEmpresa().getId().equals(SecurityUtils.getEmpresaId())) {

            throw new BusinessException("No pertenece a esta empresa");
        }
    }

    private void validarNoAutoEliminacion(Long usuarioId) {
        if (usuarioId.equals(SecurityUtils.getUsuarioId())) {
            throw new BusinessException("No puedes eliminar tu propio usuario");
        }
    }

    private void validarCambioPassword(Long usuarioId) {
        if (!esSuperAdmin() &&
            !usuarioId.equals(SecurityUtils.getUsuarioId())) {

            throw new BusinessException(
                "No puedes cambiar la contraseña de otro usuario"
            );
        }
    }

    private void validarEmpresa(Usuario usuario) {
        if (!usuario.getEmpresa().getId().equals(SecurityUtils.getEmpresaId())) {
            throw new BusinessException("No pertenece a esta empresa");
        }
    }

    private void validarPasswordActual(ChangePasswordRequest request, Usuario usuario) {
        if (!passwordEncoder.matches(
                request.getCurrentPassword(),
                usuario.getPassword())) {

            throw new BusinessException("La contraseña actual es incorrecta");
        }
    }

    private void validarNuevaPassword(String password) {
        if (password.length() < 8) {
            throw new BusinessException("Debe tener al menos 8 caracteres");
        }
    }

    // ===============================
    // 🧠 HELPERS
    // ===============================

    private Usuario obtenerUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
    }

    private Empresa obtenerEmpresa(Long id) {
        return empresaRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Empresa no encontrada"));
    }

    private Long resolveEmpresaId(Long requestEmpresaId) {
        // 🔐 Solo SUPER_ADMIN puede registrar un usuario en una empresa
        // distinta a la propia indicando empresaId en el request; el resto
        // de roles (ADMIN_EMPRESA, TECNICO, etc.) siempre queda forzado a
        // su propia empresa, sin importar lo que llegue en el request.
        if (esSuperAdmin()) {
            return requestEmpresaId != null
                    ? requestEmpresaId
                    : SecurityUtils.getEmpresaId();
        }
        return SecurityUtils.getEmpresaId();
    }

    private boolean esSuperAdmin() {
        return SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
    }

    private boolean esAdminEmpresa() {
        return SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN_EMPRESA"));
    }

    private Usuario construirUsuario(RegisterRequest request, Empresa empresa) {

        Usuario usuario = new Usuario();

        usuario.setNombre(request.getNombre());
        usuario.setEmail(request.getEmail());
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setRol(request.getRol());
        usuario.setIntentosFallidos(0);
        usuario.setActivo(true);
        usuario.setEmpresa(empresa);

        return usuario;
    }

    private void aplicarCambios(Usuario usuario, UpdateUsuarioRequest request) {

        if (request.getNombre() != null) {
            usuario.setNombre(request.getNombre());
        }

        if (request.getRol() != null) {
            usuario.setRol(request.getRol());
        }

        if (request.getActivo() != null) {
            usuario.setActivo(request.getActivo());
        }
    }
}
