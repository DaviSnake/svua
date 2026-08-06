package cl.aracridav.svua.auth.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import cl.aracridav.svua.auth.entity.RefreshToken;
import cl.aracridav.svua.auth.repository.RefreshTokenRepository;
import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.shared.exception.BusinessException;
import cl.aracridav.svua.shared.util.SecurityUtils;
import cl.aracridav.svua.usuario.entity.Usuario;
import cl.aracridav.svua.usuario.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    private final RefreshTokenRepository repository;
    private final UsuarioRepository usuarioRepository;

    /*
     * =========================================
     * CREAR
     * =========================================
     */
    @Override
    public RefreshToken createRefreshToken(Usuario usuario, Empresa empresa, String device, String ip) {

        RefreshToken token = construirToken(usuario, empresa, device, ip);

        return repository.save(token);
    }

    /*
     * =========================================
     * VALIDAR
     * =========================================
     */
    @Override
    public RefreshToken verifyExpiration(RefreshToken token) {

        if (estaExpirado(token)) {
            repository.delete(token);
            throw new BusinessException("Refresh token expirado");
        }

        return token;
    }

    /*
     * =========================================
     * ROTAR
     * =========================================
     */
    @Override
    public RefreshToken rotateToken(RefreshToken oldToken) {

        repository.delete(oldToken);

        RefreshToken nuevo = construirToken(
                oldToken.getUsuario(),
                oldToken.getEmpresa(),
                oldToken.getDevice(),
                oldToken.getIp()
        );

        return repository.save(nuevo);
    }

    /*
     * =========================================
     * ELIMINACIÓN
     * =========================================
     */
    @Override
    public void deleteToken(RefreshToken token) {
        repository.delete(token);
    }

    @Override
    public void deleteByUsuario(Usuario usuario) {
        repository.deleteByUsuario(usuario);
    }

    @Override
    @Transactional
    public void logoutUsuarioActual() {

        Usuario usuario = obtenerUsuarioActual();

        repository.deleteByUsuario(usuario);
    }

    /*
     * =========================================
     * HELPERS
     * =========================================
     */

    private RefreshToken construirToken(Usuario usuario, Empresa empresa, String device, String ip) {

        RefreshToken token = new RefreshToken();

        token.setUsuario(usuario);
        token.setEmpresa(empresa);
        token.setToken(generarToken());
        token.setExpiryDate(calcularExpiracion());
        token.setDevice(device);
        token.setIp(ip);

        return token;
    }

    private boolean estaExpirado(RefreshToken token) {
        return token.getExpiryDate().isBefore(Instant.now());
    }

    private String generarToken() {
        return UUID.randomUUID().toString();
    }

    private Instant calcularExpiracion() {
        return Instant.now().plusMillis(refreshExpiration);
    }

    private Usuario obtenerUsuarioActual() {
        return usuarioRepository.findById(SecurityUtils.getUsuarioId())
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
    }
}