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
import cl.aracridav.svua.multitenancy.RlsContextService;
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
    private final RlsContextService rlsContextService;

    @Value("${app.frontend.url}")
    private String frontendUrl; // 👈 AQUÍ, fuera del método

    // 🔐 Mensaje unico para TODO el flujo de login previo a autenticar con
    // exito: antes, usuario-no-existe / usuario-inactivo / empresa-inactiva
    // / plan-vencido / password-incorrecta devolvian mensajes DISTINTOS,
    // lo que permitia a un atacante enumerar que emails/usuarios existen
    // en el sistema probando uno por uno. El detalle real del motivo sigue
    // quedando registrado server-side (comentarios/logs), solo el mensaje
    // que ve el cliente se unifico.
    private static final String CREDENCIALES_INVALIDAS = "Credenciales inválidas";

    @Transactional
    @PostMapping("/login")
    public AuthLoginResponse login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {

        Authentication auth;

        String tokenJti = UUID.randomUUID().toString();

        String ip = obtenerIp(httpRequest);

        // 🔒 Todavia no se sabe a que empresa pertenece este email (es
        // justo lo que esta consulta busca averiguar): sin bypass, Row
        // Level Security la bloquearia por completo (0 filas), aunque
        // el email exista.
        rlsContextService.aplicarBypass();

        Usuario usuario = usuarioRepository
            .findByEmailWithEmpresa(request.getEmail())
            .orElseThrow(() ->
                    new BusinessException(CREDENCIALES_INVALIDAS));

        // 🔓 Verificar desbloqueo automático
        verificarDesbloqueoAutomatico(usuario);

        // 🔒 1️⃣ Usuario activo (antes: "Usuario inactivo", revelaba que el
        // usuario existe)
        if (!usuario.getActivo()) {
                throw new BusinessException(CREDENCIALES_INVALIDAS);
        }

        Empresa empresa = usuario.getEmpresa();

        // 🔒 2️⃣ Empresa activa (antes: "La empresa se encuentra Inactiva",
        // revelaba existencia de usuario/empresa)
        if (!empresa.getActiva()) {
                throw new BusinessException(CREDENCIALES_INVALIDAS);
        }

        // 🔒 3️⃣ Validar vencimiento de plan (antes: "El Plan de la Empresa
        // ha vencido", revelaba existencia de usuario/empresa)
        if (empresa.getFechaFinPlan().isBefore(LocalDate.now())) {

                empresa.setActiva(false);
                empresaRepository.save(empresa);

                throw new BusinessException(CREDENCIALES_INVALIDAS);
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
                // antes: "Correo o contraseña incorrectos", mensaje distinto
                // al de usuario-no-existe -- permitia diferenciar por texto.
                throw new BusinessException(CREDENCIALES_INVALIDAS);
        }

        // 🔹 Generar principal
        UsuarioPrincipal userPrincipal =
            (UsuarioPrincipal) auth.getPrincipal();

        // 🔐 Validar contraseña
        if (!passwordEncoder.matches(
                request.getPassword(),
                usuario.getPassword())) {

                throw new BusinessException(CREDENCIALES_INVALIDAS);
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

        // 🔒 Mismo motivo que en login(): el refresh token llega como un
        // string crudo, sin saber a que empresa pertenece hasta despues
        // de encontrarlo.
        rlsContextService.aplicarBypass();

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

    @Transactional
    @PostMapping("/request-reset")
    public ResponseEntity<?> requestReset(@RequestBody EmailResetRequest request,
                                        HttpServletRequest httpRequest) {

        // 🔐 Antes: si el email no existía, lanzaba BusinessException
        // ("Usuario no encontrado") -- enumeración directa y trivial de
        // cuentas existentes. Ahora la respuesta es SIEMPRE la misma
        // (200, mismo mensaje genérico), exista o no el email; el envío
        // real del correo solo ocurre si el usuario existe.
        //
        // 🔒 Mismo motivo que en login(): todavia no se sabe a que
        // empresa pertenece este email.
        rlsContextService.aplicarBypass();

        usuarioRepository.findByEmail(request.getEmail()).ifPresent(user -> {
                String token = usuarioService.createToken(user);
                String link = frontendUrl + "/reset-password?token=" + token;
                emailService.sendResetEmail(request.getEmail(), link);
        });

        return ResponseEntity.ok(Map.of(
                "message", "Si el correo existe en nuestro sistema, se enviará un enlace de recuperación"));
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
