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
import org.springframework.util.StringUtils;

import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.empresa.entity.TipoPlan;
import cl.aracridav.svua.empresa.repository.EmpresaRepository;
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
                               @Value("${app.init-data.admin-email:admin@admin.com}") String adminEmail,
                               @Value("${app.init-data.admin-password:}") String adminPassword) {
        return args -> {

            if (empresaRepo.count() == 0) {

                if (!StringUtils.hasText(adminPassword)) {
                    log.warn("app.init-data.enabled=true pero no se definió app.init-data.admin-password. " +
                             "Se omite la creación de datos iniciales.");
                    return;
                }

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
        };
    }

}
