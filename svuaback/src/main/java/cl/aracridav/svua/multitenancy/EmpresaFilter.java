package cl.aracridav.svua.multitenancy;

import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cl.aracridav.svua.config.security.UsuarioPrincipal;
import cl.aracridav.svua.shared.util.SecurityUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Component
public class EmpresaFilter {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private RlsContextService rlsContextService;

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
            // 🔒 mismo criterio que ya tenia esta linea para el filtro
            // de Hibernate (SUPER_ADMIN queda exento), pero ahora
            // tambien se lo informa a Postgres (ver migracion V27):
            // sin esto, las consultas cross-empresa de SUPER_ADMIN
            // (ej. obtenerInformeMantenciones sin empresaId) quedarian
            // en cero filas por Row Level Security.
            rlsContextService.aplicarBypass();
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

        // 🔒 mismo empresaId que se le acaba de pasar al @Filter de
        // Hibernate, pero para la policy de Row Level Security de
        // Postgres (ver migracion V27 y RlsContextService).
        rlsContextService.aplicarEmpresa(empresaId);
    }

}
