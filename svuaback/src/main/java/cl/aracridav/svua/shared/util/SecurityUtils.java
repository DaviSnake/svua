package cl.aracridav.svua.shared.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import cl.aracridav.svua.config.security.UsuarioPrincipal;
import cl.aracridav.svua.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    public static Long getEmpresaId() {

        Authentication auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof UsuarioPrincipal)) {
            throw new BusinessException("Usuario no autenticado");
        }

        UsuarioPrincipal principal =
                (UsuarioPrincipal) auth.getPrincipal();

        return principal.getEmpresaId();
    }

    public static Long getUsuarioId() {

        Authentication auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof UsuarioPrincipal)) {
            throw new BusinessException("Usuario no autenticado");
        }

        UsuarioPrincipal principal = (UsuarioPrincipal) auth.getPrincipal();

         return principal.getId();
    }

    public static boolean esSuperAdmin() {

        Authentication auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        return auth.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_SUPER_ADMIN"));
    }

}
