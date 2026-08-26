package cl.aracridav.svua.shared.auditoria;

import org.hibernate.envers.RevisionListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import cl.aracridav.svua.config.security.UsuarioPrincipal;

// 🔎 Envers instancia esta clase el mismo (con new, via reflection —
// NO es un bean de Spring, no se puede inyectar nada aca), y llama
// newRevision() automaticamente antes de guardar cada _aud. Mismo
// criterio que UsuarioAuditorAware (BaseEntity) para resolver el
// usuario autenticado actual desde el SecurityContext.
public class RevisionInfoListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {

        RevisionInfo revision = (RevisionInfo) revisionEntity;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UsuarioPrincipal principal) {
            revision.setUsuarioId(principal.getId());
            revision.setEmpresaId(principal.getEmpresaId());
        }
    }
}
