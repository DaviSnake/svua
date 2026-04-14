package cl.aracridav.svua.usuario.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import cl.aracridav.svua.auth.dto.request.ChangePasswordRequest;
import cl.aracridav.svua.usuario.dto.request.RegisterRequest;
import cl.aracridav.svua.usuario.dto.request.UpdateUsuarioRequest;
import cl.aracridav.svua.usuario.dto.response.UsuarioResponse;

public interface UsuarioService {
    
    public UsuarioResponse registrarUsuario(RegisterRequest request);

    public UsuarioResponse actualizarUsuario(Long usuarioId, UpdateUsuarioRequest request);
    
    public void eliminarUsuario(Long usuarioId);

    public void cambiarPassword(Long usuarioId, ChangePasswordRequest request);

    public Page<UsuarioResponse> listarUsuarios(Pageable pageable);

}
