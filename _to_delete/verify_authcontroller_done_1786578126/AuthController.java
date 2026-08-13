package cl.aracridav.svua.auth.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.aracridav.svua.auth.dto.request.EmailResetRequest;
import cl.aracridav.svua.auth.dto.request.LoginRequest;
import cl.aracridav.svua.auth.dto.request.RefreshTokenRequest;
import cl.aracridav.svua.auth.dto.request.ResetPasswordRequest;
import cl.aracridav.svua.auth.dto.response.AuthLoginResponse;
import cl.aracridav.svua.auth.entity.RefreshToken;
import cl.aracridav.svua.auth.repository.RefreshTokenRepository;
import cl.aracridav.svua.auth.service.RefreshTokenService;
import cl.aracridav.svua.config.security.JwtService;
import cl.aracridav.svua.config.security.UsuarioPrincipal;
import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.empresa.repository.EmpresaRepository;
import cl.aracridav.svua.shared.constants.AppConstants;
import cl.aracridav.svua.shared.exception.BusinessException;
import cl.aracridav.svua.shared.exception.InvalidRefreshTokenException;
import cl.aracridav.svua.shared.service.EmailService;
import cl.aracridav.svua.usuario.entity.Usuario;
import cl.aracridav.svua.usuario.repository.UsuarioRepository;
import cl.aracridav.svua.usuario.service.SesionUsuarioService;
import cl.aracridav.svua.usuario.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/svua/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final SesionUsuarioService sesionUsuarioService;
    private final EmailService emailService;
    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmpresaRepository empresaRepository;

    @Value("${app.frontend.url}")
    private String frontendUrl; // 👈 AQUÍ, fuera del método

    @PostMapping("/login")
    public AuthLoginResponse login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {

        Authentication auth;

        String tokenJti = UUID.randomUUID().toString();

        String ip = obtenerIp(httpRequest);

        Usuario usuario = usuarioRepository
            .findByEmailWithEmpresa(request.getEmail())
            .orElseThrow(() ->
                    new BusinessException("Credenciales inválidas"));

        // 🔓 Verificar desbloqueo automático
        verificarDesbloqueoAutomatico(usuario);

        // 🔒 1️⃣ Usuario activo
        if (!usuario.getActivo()) {
                throw new BusinessException("Usuario inactivo");
        }

        Empresa empresa = usuario.getEmpresa();

        // 🔒 2️⃣ Empresa activa
        if (!empresa.getActiva()) {
                throw new BusinessException(
                        "La empresa se encuentra Inactiva");
        }

        // 🔒 3️⃣ Validar vencimiento de plan
        if (empresa.getFechaFinPlan().isBefore(LocalDate.now())) {

                empresa.setActiva(false);
                empresaRepository.save(empresa);

                throw new BusinessException(
                        "El Plan de la Empresa ha vencido");
        }

        try{
                auth = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.getEmail(),
                                request.getPassword()
                        )
                );

                // ✅ Login correcto → resetear intentos
                usuario.setIntentosFallidos(0);
                usuario.setFechaBloqueo(null);
                usuarioRepository.save(usuario);

        } catch (BadCredentialsException e) {
                manejarIntentoFallido(usuario);
                throw new BusinessException("Correo o contraseña incorrectos");
        }

        // 🔹 Generar principal
        UsuarioPrincipal userPrincipal =
            (UsuarioPrincipal) auth.getPrincipal();

        // 🔐 Validar contraseña
        if (!passwordEncoder.matches(
                request.getPassword(),
                usuario.getPassword())) {

                throw new BusinessException("Credenciales inválidas");
        }


        String accessToken = jwtService.generateToken(userPrincipal, tokenJti);

        // Datos del dispositivo
        String device = httpRequest.getHeader("User-Agent");

        RefreshToken refreshToken =
            refreshTokenService.createRefreshToken(usuario, empresa, device, ip);

        sesionUsuarioService.crearSesion(
            usuario,
            tokenJti,
            ip,
            request.getNavegador(),
            request.getSistemaOperativo(),
            request.getDispositivo(),
            request.getVersionApp()
        );

        return AuthLoginResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken.getToken())
            .build();
    }

    @Transactional
    @PostMapping("/refresh")
    public AuthLoginResponse refreshToken(@RequestBody RefreshTokenRequest request,
         HttpServletRequest httpRequest
    ) {

        RefreshToken oldToken = refreshTokenRepository
            .findByToken(request.getRefreshToken())
            .map(refreshTokenService::verifyExpiration)
            .orElseThrow(() ->
                new InvalidRefreshTokenException("Refresh token inválido o expirado"));

        Usuario usuario = oldToken.getUsuario();

        // 🔥 Eliminar token viejo (ROTACIÓN)
        refreshTokenService.deleteToken(oldToken);

        // Datos del dispositivo
        String device = httpRequest.getHeader("User-Agent");
        String ip = httpRequest.getRemoteAddr();

        // 🔥 Crear nuevo refresh token
        RefreshToken newRefreshToken =
                refreshTokenService.createRefreshToken(usuario, usuario.getEmpresa(), device, ip);

        // 🔥 Crear nuevo access token.
        // 🐛 FIX: antes se llamaba jwtService.generateToken(principal) (el
        // overload de 1 argumento), que NO agrega el claim "userName" que
        // JwtAuthenticationFilter necesita para autenticar al usuario. El
        // access token que devolvía este endpoint quedaba "roto": la
        // siguiente petición del usuario llegaba sin sesión (401) aunque el
        // refresh hubiera respondido 200, dando la sensación de que el
        // refresh "se caía". Se usa el mismo overload que en login(), con un
        // tokenJti nuevo.
        String newTokenJti = UUID.randomUUID().toString();

        UsuarioPrincipal principal =
                new UsuarioPrincipal(usuario);

        String newAccessToken =
                jwtService.generateToken(principal, newTokenJti);

        return AuthLoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .build();
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {

        refreshTokenService.logoutUsuarioActual();

        SecurityContextHolder.clearContext();

        return ResponseEntity.ok().build();
    }

    private void manejarIntentoFallido(Usuario usuario) {

        int nuevosIntentos = usuario.getIntentosFallidos() + 1;
        usuario.setIntentosFallidos(nuevosIntentos);

        if (nuevosIntentos >= AppConstants.MAX_INTENTOS) {
                usuario.setActivo(false);
                usuario.setFechaBloqueo(LocalDateTime.now());
        }

        usuarioRepository.save(usuario);
    }

    @PostMapping("/request-reset")
    public ResponseEntity<?> requestReset(@RequestBody EmailResetRequest request,
                                        HttpServletRequest httpRequest) {


        Usuario user = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));

        String token = usuarioService.createToken(user);

        String link = frontendUrl + "/reset-password?token=" + token;

        emailService.sendResetEmail(request.getEmail(), link);

        return ResponseEntity.ok(Map.of("message", "Correo enviado"));
    }

    @GetMapping("/validate-token")
    public ResponseEntity<?> validateToken(@RequestParam String token) {

        usuarioService.validateToken(token);

        return ResponseEntity.ok(Map.of("message", "Token válido"));
    }

    // 3️⃣ Cambiar contraseña
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {

        usuarioService.resetPassword(request.getToken(), request.getPassword());

        return ResponseEntity.ok(Map.of("message", "Contraseña actualizada"));
    }


    private void verificarDesbloqueoAutomatico(Usuario usuario) {

        if (!usuario.getActivo()
                && usuario.getFechaBloqueo() != null) {

                LocalDateTime desbloqueo =
                        usuario.getFechaBloqueo().plusMinutes(AppConstants.MINUTOS_BLOQUEO);

                if (LocalDateTime.now().isAfter(desbloqueo)) {

                usuario.setActivo(true);
                usuario.setIntentosFallidos(0);
                usuario.setFechaBloqueo(null);

                usuarioRepository.save(usuario);
                }
        }
    }

    private String obtenerIp(HttpServletRequest request) {

        String ip = request.getHeader("X-Forwarded-For");

        if (ip != null && !ip.isBlank()) {
                return ip.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

}
