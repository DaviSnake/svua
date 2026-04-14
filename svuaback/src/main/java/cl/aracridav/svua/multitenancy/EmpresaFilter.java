package cl.aracridav.svua.multitenancy;

import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import cl.aracridav.svua.config.security.UsuarioPrincipal;
import cl.aracridav.svua.shared.util.SecurityUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Component
public class EmpresaFilter {

    @PersistenceContext
    private EntityManager entityManager;

    public void activarFiltroEmpresa() {

        Authentication authentication =
            SecurityContextHolder.getContext().getAuthentication();

        // Si no hay autenticación
        if (authentication == null || !authentication.isAuthenticated()) {
            return;
        }

        // Si es usuario anónimo
        if (authentication.getPrincipal().equals("anonymousUser")) {
            return;
        }

        if (SecurityUtils.esSuperAdmin()) {
            return; // no aplicar filtro
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof UsuarioPrincipal usuario)) {
            return;
        }

        Long empresaId = usuario.getEmpresaId();

        Session session = entityManager.unwrap(Session.class);

        Filter filter = session.enableFilter("empresaFilter");
        filter.setParameter("empresaId", empresaId);
    }

}
