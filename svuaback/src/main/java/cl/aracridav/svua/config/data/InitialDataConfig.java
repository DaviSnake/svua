package cl.aracridav.svua.config.data;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.empresa.entity.TipoPlan;
import cl.aracridav.svua.empresa.repository.EmpresaRepository;
import cl.aracridav.svua.shared.enums.RolUsuario;
import cl.aracridav.svua.usuario.entity.Usuario;
import cl.aracridav.svua.usuario.repository.UsuarioRepository;

@Configuration
public class InitialDataConfig {

    @Bean
    CommandLineRunner initData(EmpresaRepository empresaRepo,
                               UsuarioRepository usuarioRepo,
                               PasswordEncoder encoder) {
        return args -> {

            if (empresaRepo.count() == 0) {

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
                usuario.setEmail("admin@admin.com");
                usuario.setPassword(encoder.encode("Admin123*"));
                usuario.setRol(RolUsuario.SUPER_ADMIN);
                usuario.setIntentosFallidos(0);
                usuario.setFechaBloqueo(null);
                usuario.setActivo(true);
                usuario.setEmpresa(empresa);

                usuarioRepo.save(usuario);

                System.out.println("✅ EMPRESA creada automáticamente");
                System.out.println("✅ SUPER_ADMIN creado automáticamente");
            }
        };
    }

}
