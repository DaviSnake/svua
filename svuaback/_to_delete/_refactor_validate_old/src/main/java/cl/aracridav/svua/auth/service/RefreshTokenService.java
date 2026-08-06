package cl.aracridav.svua.auth.service;

import cl.aracridav.svua.auth.entity.RefreshToken;
import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.usuario.entity.Usuario;

public interface RefreshTokenService {

    public RefreshToken createRefreshToken(Usuario usuario, Empresa empresa, String device, String ip);

    public RefreshToken verifyExpiration(RefreshToken token);

    public void deleteToken(RefreshToken token);

    public RefreshToken rotateToken(RefreshToken oldToken);

    public void deleteByUsuario(Usuario usuario);

    public void logoutUsuarioActual();

}
