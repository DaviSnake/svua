package cl.aracridav.svua.multitenancy;

import java.sql.PreparedStatement;

import org.hibernate.Session;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

// 🔒 Row Level Security (Postgres): centraliza el "SET" de los dos
// parametros de sesion que leen las policies creadas en la migracion
// V27 (empresa_isolation). Esto complementa al @Filter de Hibernate
// "empresaFilter" (ver EmpresaFilter/EmpresaFilterAspect): ese filtro
// evita que Hibernate TRAIGA filas de otra empresa, pero sigue siendo
// una regla de aplicacion; esto hace que Postgres MISMO no las
// devuelva ni las deje escribir, aunque una query se olvide filtrar.
//
// Se llama desde:
//   - EmpresaFilter.activarFiltroEmpresa(): en cada request autenticado
//     y en cada metodo de la capa de servicio (via EmpresaFilterAspect).
//   - Los schedulers que corren fuera de un request HTTP (no hay
//     Authentication en el SecurityContext ahi, asi que EmpresaFilter
//     nunca se los aplica): ver MantencionScheduler.
@Component
public class RlsContextService {

    @PersistenceContext
    private EntityManager entityManager;

    // Usuario normal: solo ve/puede escribir filas de su propia empresa.
    public void aplicarEmpresa(Long empresaId) {
        setConfig("app.current_empresa_id", empresaId != null ? String.valueOf(empresaId) : "");
        setConfig("app.bypass_rls", "off");
    }

    // SUPER_ADMIN (ya exento del @Filter de Hibernate, ver
    // EmpresaFilter) y jobs internos de sistema que deben operar sobre
    // mas de una empresa a la vez.
    public void aplicarBypass() {
        setConfig("app.current_empresa_id", "");
        setConfig("app.bypass_rls", "on");
    }

    private void setConfig(String parametro, String valor) {
        Session session = entityManager.unwrap(Session.class);
        session.doWork(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("SELECT set_config(?, ?, false)")) {
                ps.setString(1, parametro);
                ps.setString(2, valor);
                ps.execute();
            }
        });
    }
}
