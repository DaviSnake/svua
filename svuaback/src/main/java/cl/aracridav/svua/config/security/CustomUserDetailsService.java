package cl.aracridav.svua.config.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.aracridav.svua.multitenancy.RlsContextService;
import cl.aracridav.svua.usuario.entity.Usuario;
import cl.aracridav.svua.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final RlsContextService rlsContextService;

    // 🔒 Se busca por email SIN saber todavia a que empresa pertenece
    // el usuario (login), o reconstruyendo el principal desde el JWT en
    // CADA request autenticado (JwtAuthenticationFilter llama esto por
    // request, no solo al iniciar sesion). En ambos casos hay que cruzar
    // empresas para encontrar la fila, asi que hace falta bypass_rls
    // antes de esta consulta puntual -- nadie mas lo fija en este punto
    // del pipeline (EmpresaFilterAspect solo intercepta metodos de la
    // capa de servicio, y este filtro corre ANTES de llegar ahi).
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        rlsContextService.aplicarBypass();

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        return new UsuarioPrincipal(usuario);
    }
}
