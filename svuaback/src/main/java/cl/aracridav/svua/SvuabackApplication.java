package cl.aracridav.svua;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAspectJAutoProxy
@SpringBootApplication
@EnableScheduling
// 🔥 habilita @CreatedDate/@LastModifiedDate/@CreatedBy/@LastModifiedBy
// (ver BaseEntity). Sin esto, esas anotaciones quedan declaradas pero
// nunca se ejecutan. auditorAwareRef apunta a UsuarioAuditorAware, que
// resuelve el usuario autenticado actual para @CreatedBy/@LastModifiedBy
// (@CreatedDate/@LastModifiedDate no lo necesitan, se llenan solos).
@EnableJpaAuditing(auditorAwareRef = "usuarioAuditorAware")
@EnableSpringDataWebSupport(
    pageSerializationMode =
        EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO
)
public class SvuabackApplication {

	public static void main(String[] args) {
		SpringApplication.run(SvuabackApplication.class, args);
	}

}
