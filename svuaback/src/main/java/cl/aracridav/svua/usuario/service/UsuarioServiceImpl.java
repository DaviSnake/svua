package cl.aracridav.svua.usuario.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import cl.aracridav.svua.auth.dto.request.ChangePasswordRequest;
import cl.aracridav.svua.config.security.SecurityService;
import cl.aracridav.svua.config.security.UsuarioPrincipal;
import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.empresa.repository.EmpresaRepository;
import cl.aracridav.svua.shared.enums.RolUsuario;
import cl.aracridav.svua.shared.exception.BusinessException;
import cl.aracridav.svua.shared.mappers.GeneralMapper;
import cl.aracridav.svua.shared.util.SecurityUtils;
import cl.aracridav.svua.usuario.dto.request.RegisterRequest;
import cl.aracridav.svua.usuario.dto.request.UpdateUsuarioRequest;
import cl.aracridav.svua.usuario.dto.response.UsuarioResponse;
import cl.aracridav.svua.usuario.entity.Usuario;
import cl.aracridav.svua.usuario.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder passwordEncoder;
    private final GeneralMapper generalMapper;
    private final SecurityService securityService;

    @Override
    public UsuarioResponse registrarUsuario(RegisterRequest request) {     
        
        Long empresaId = SecurityUtils.getEmpresaId();

        if (request.getEmpresaId() != null){
            empresaId = request.getEmpresaId();
        }

        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("El email ya está registrado");
        }

        if (request.getRol() == RolUsuario.SUPER_ADMIN) {
            throw new BusinessException("No se permite crear usuarios SUPER ADMIN");
        }

        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() ->
                    new BusinessException("Empresa no encontrado"));

        Usuario usuario = new Usuario();
                usuario.setNombre(request.getNombre());
                usuario.setEmail(request.getEmail());
                usuario.setPassword(passwordEncoder.encode(request.getPassword()));
                usuario.setRol(request.getRol());
                usuario.setIntentosFallidos(0);
                usuario.setActivo(true);
                usuario.setEmpresa(empresa);

        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        return generalMapper.mapUsuarioToResponse(usuarioGuardado);
    }

    @Override
    public UsuarioResponse actualizarUsuario(
        Long usuarioId,
        UpdateUsuarioRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Long empresaId = SecurityUtils.getEmpresaId();

        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() ->
                    new BusinessException("Usuario no encontrado"));
        
        boolean esSuperAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));

        // 🔐 Validar empresa
        if (!usuario.getEmpresa().getId().equals(empresaId) && !esSuperAdmin) {
            throw new BusinessException("No pertenece a esta empresa");
        }

        // 🚫 No permitir modificar SUPER_ADMIN
        if (usuario.getRol() == RolUsuario.SUPER_ADMIN) {
            throw new BusinessException(
                    "No se puede modificar un SUPER ADMIN");
        }

        // 🚫 Evitar escalamiento de privilegios
        UsuarioPrincipal actual =
                (UsuarioPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        if (actual.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"))) {

            // Si no es SUPER_ADMIN no puede asignar SUPER_ADMIN
            if (request.getRol() == RolUsuario.SUPER_ADMIN) {
                throw new BusinessException(
                        "No puedes asignar rol SUPER_ADMIN");
            }
        }

        // 🔄 Actualizar datos permitidos
        if (request.getNombre() != null) {
            usuario.setNombre(request.getNombre());
        }

        if (request.getRol() != null) {
            usuario.setRol(request.getRol());
        }

        if (request.getActivo() != null) {
            usuario.setActivo(request.getActivo());
        }

        Usuario usuarioActualizado = usuarioRepository.save(usuario);

        return generalMapper.mapUsuarioToResponse(usuarioActualizado);
    }

    @Override
    public void eliminarUsuario(Long usuarioId) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Long empresaId = SecurityUtils.getEmpresaId();

        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() ->
                    new BusinessException("Usuario no encontrado"));

        boolean esSuperAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));

        // 🔐 Validar que pertenece a la empresa
        if (!usuario.getEmpresa().getId().equals(empresaId) && !esSuperAdmin) {
            throw new BusinessException("No pertenece a esta empresa");
        }

        // 🚫 No permitir eliminar SUPER_ADMIN
        if (usuario.getRol() == RolUsuario.SUPER_ADMIN) {
            throw new BusinessException(
                    "No se puede eliminar un SUPER ADMIN");
        }

        // 🚫 No permitir eliminarse a sí mismo
        UsuarioPrincipal actual =
                (UsuarioPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        if (usuario.getId().equals(actual.getId())) {
            throw new BusinessException(
                    "No puedes eliminar tu propio usuario");
        }
            usuario.setActivo(false);
            usuarioRepository.save(usuario);
    }

    @Override
    public void cambiarPassword(
        Long usuarioId,
        ChangePasswordRequest request) {

        Long empresaId = SecurityUtils.getEmpresaId();

        UsuarioPrincipal principal =
                (UsuarioPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        Usuario usuarioAuth = securityService.obtenerUsuarioAutenticado();

        String rol = usuarioAuth.getRol().name();

        // 🔐 Solo puede cambiar su propia contraseña
        if (!principal.getId().equals(usuarioId) && !rol.equals("SUPER_ADMIN")) {
            throw new BusinessException(
                    "No puedes cambiar la contraseña de otro usuario");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() ->
                        new BusinessException("Usuario no encontrado"));

        // 🔐 Validar empresa (multi-tenant)
        if (!usuario.getEmpresa().getId().equals(empresaId)) {
            throw new BusinessException(
                    "No pertenece a esta empresa");
        }

        // 🔐 Validar contraseña actual
        if (!passwordEncoder.matches(
                request.getCurrentPassword(),
                usuario.getPassword())) {

            throw new BusinessException(
                    "La contraseña actual es incorrecta");
        }

        // 🔐 Validar nueva contraseña mínima
        if (request.getNewPassword().length() < 8) {
            throw new BusinessException(
                    "La nueva contraseña debe tener al menos 8 caracteres");
        }

        // 🔒 Encriptar nueva contraseña
        usuario.setPassword(
                passwordEncoder.encode(request.getNewPassword()));

        usuarioRepository.save(usuario);
    }

    public Page<UsuarioResponse> listarUsuarios(Pageable pageable) {

        Page<Usuario> usuarios;

        Long empresaId = SecurityUtils.getEmpresaId();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        boolean esSuperAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        
        boolean esAdminEmpresa = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN_EMPRESA"));

        if (esSuperAdmin) {
            usuarios = usuarioRepository.findAll(pageable);

        } else if (esAdminEmpresa) {
            usuarios = usuarioRepository.findByEmpresaId(empresaId, pageable);

        } else {
            Usuario usuario = usuarioRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            usuarios = new PageImpl<>(List.of(usuario));
        }

        return usuarios.map(generalMapper::mapUsuarioToResponse);
    }
}
