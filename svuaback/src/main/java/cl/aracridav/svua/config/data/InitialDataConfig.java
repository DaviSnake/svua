package cl.aracridav.svua.config.data;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.empresa.entity.TipoPlan;
import cl.aracridav.svua.empresa.repository.EmpresaRepository;
import cl.aracridav.svua.multitenancy.RlsContextService;
import cl.aracridav.svua.shared.enums.RolUsuario;
import cl.aracridav.svua.usuario.entity.Usuario;
import cl.aracridav.svua.usuario.repository.UsuarioRepository;

/**
 * Crea empresa y SUPER_ADMIN de arranque SOLO cuando app.init-data.enabled=true.
 * Por defecto está deshabilitado: NUNCA debe activarse en producción sin
 * definir un email/password propios via variables de entorno.
 */
@Configuration
@ConditionalOnProperty(name = "app.init-data.enabled", havingValue = "true")
public class InitialDataConfig {

    private static final Logger log = LoggerFactory.getLogger(InitialDataConfig.class);

    @Bean
    CommandLineRunner initData(EmpresaRepository empresaRepo,
                               UsuarioRepository usuarioRepo,
                               PasswordEncoder encoder,
                               RlsContextService rlsContextService,
                               PlatformTransactionManager txManager,
                               @Value("${app.init-data.admin-email:admin@admin.com}") String adminEmail,
                               @Value("${app.init-data.admin-password:}") String adminPassword) {
        return args -> {

            // 🔒 Este CommandLineRunner corre al arrancar, fuera de
            // cualquier request HTTP -- no hay Authentication en el
            // SecurityContext, asi que EmpresaFilter nunca activa el
            // contexto de RLS aca (ver RlsContextService). Sin este
            // bypass explicito, "app.current_empresa_id" queda sin
            // definir: empresaRepo.count() lo ve TODO como 0 filas
            // (falso positivo de "base vacia" aunque ya existan
            // empresas) y, peor, el propio empresaRepo.save() de mas
            // abajo falla con "new row violates row-level security
            // policy" -- la politica exige bypass_rls=on o que
            // empresa_id coincida con current_empresa_id, y ninguno de
            // los dos esta seteado. Mismo patron que usan los
            // schedulers (ver MantencionScheduler).
            TransactionTemplate tx = new TransactionTemplate(txManager);
            tx.executeWithoutResult(status -> {

                rlsContextService.aplicarBypass();

                if (empresaRepo.count() == 0) {

                    if (!StringUtils.hasText(adminPassword)) {
                        log.warn("app.init-data.enabled=true pero no se definió app.init-data.admin-password. " +
                                 "Se omite la creación de datos iniciales.");
                        return;
                    }

                    // 🔒 Mismos flags booleanos NOT NULL que construirEmpresaBase()
                    // setea al crear una empresa desde la pantalla SUPER_ADMIN (ver
                    // EmpresaServiceImpl) -- todos en false para una empresa nueva
                    // real: nada de datos demo, ni modulos opt-in habilitados por
                    // defecto. Sin esto, empresaRepo.save() falla con "null value
                    // in column ... violates not-null constraint" (la columna tiene
                    // DEFAULT en la BD, pero Hibernate igual inserta el valor NULL
                    // explicito del campo Java, sin caer al default de la columna).
                    Empresa empresa = new Empresa();
                    empresa.setNombre("Casa Matriz SPA");
                    empresa.setRut("99.999.999-9");
                    empresa.setEmailContacto("contacto@casamatriz.cl");
                    empresa.setTelefono("+56912345678");
                    empresa.setDireccion("Av. Casa Matriz 1234");
                    empresa.setTipoPlan(TipoPlan.ENTERPRISE);
                    empresa.setMaxUsuarios(999);
                    empresa.setMaxActivos(999);
                    empresa.setActiva(true);
                    empresa.setFechaCreacion(LocalDateTime.now());
                    empresa.setFechaFinPlan(LocalDate.now().plusYears(10));
                    empresa.setDemo(false);
                    empresa.setCodigoQrHabilitado(false);
                    empresa.setCodigoEan13Habilitado(false);
                    empresa.setControlTurnoHabilitado(false);
                    empresa.setHojaControlHabilitado(false);
                    empresa.setInformeMantencionesHabilitado(false);
                    empresaRepo.save(empresa);

                    Usuario usuario = new Usuario();
                    usuario.setNombre("Admin Sistema");
                    usuario.setEmail(adminEmail);
                    usuario.setPassword(encoder.encode(adminPassword));
                    usuario.setRol(RolUsuario.SUPER_ADMIN);
                    usuario.setIntentosFallidos(0);
                    usuario.setFechaBloqueo(null);
                    usuario.setActivo(true);
                    usuario.setEmpresa(empresa);

                    usuarioRepo.save(usuario);

                    log.info("EMPRESA inicial creada automáticamente (app.init-data.enabled=true)");
                    log.info("SUPER_ADMIN inicial creado automáticamente con email {}", adminEmail);
                }
            });
        };
    }

}
