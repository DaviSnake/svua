package cl.aracridav.svua.config.security;

import java.util.Optional;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

// 🔎 Provee el "actor" que completa @CreatedBy/@LastModifiedBy en
// BaseEntity (creadoPorId/modificadoPorId, ver ese archivo). Registrado
// en @EnableJpaAuditing(auditorAwareRef = "usuarioAuditorAware") de
// SvuabackApplication.
//
// Fuera de un request HTTP autenticado -- schedulers, jobs internos de
// sistema, ver RlsContextService/MantencionScheduler -- no hay
// Authentication en el SecurityContext, y estos campos quedan en NULL:
// se interpreta como "sistema", no como un dato faltante por error.
@Component
public class UsuarioAuditorAware implements AuditorAware<Long> {

    @Override
    public Optional<Long> getCurrentAuditor() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UsuarioPrincipal principal)) {
            return Optional.empty();
        }

        return Optional.of(principal.getId());
    }
}
