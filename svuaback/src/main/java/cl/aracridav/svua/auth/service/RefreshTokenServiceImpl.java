package cl.aracridav.svua.auth.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import cl.aracridav.svua.auth.entity.RefreshToken;
import cl.aracridav.svua.auth.repository.RefreshTokenRepository;
import cl.aracridav.svua.empresa.entity.Empresa;
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

    public RefreshToken createRefreshToken(Usuario usuario, Empresa empresa, String device, String ip) {

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUsuario(usuario);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshExpiration));
        refreshToken.setEmpresa(empresa);
        refreshToken.setDevice(device);
        refreshToken.setIp(ip);

        return repository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            repository.delete(token);
            throw new RuntimeException("Refresh token expirado");
        }
        return token;
    }

    public RefreshToken rotateToken(RefreshToken oldToken) {

        repository.delete(oldToken);

        RefreshToken newToken = new RefreshToken();

        newToken.setUsuario(oldToken.getUsuario());
        newToken.setToken(UUID.randomUUID().toString());
        newToken.setExpiryDate(
                Instant.now().plusMillis(refreshExpiration)
        );

        newToken.setDevice(oldToken.getDevice());
        newToken.setIp(oldToken.getIp());

        return repository.save(newToken);
    }

     public void deleteToken(RefreshToken token) {
        repository.delete(token);
    }

    public void deleteByUsuario(Usuario usuario) {

        repository.deleteByUsuario(usuario);
    }

    @Transactional
    public void logoutUsuarioActual() {

        Long usuarioId = SecurityUtils.getUsuarioId();

        Optional<Usuario> usuario = usuarioRepository.findById(usuarioId);

        repository.deleteByUsuario(usuario);
    }
}
