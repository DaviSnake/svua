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
// 🔥 habilita @CreatedDate/@LastModifiedDate (ver BaseEntity.fechaCreacion).
// Sin esto, esas anotaciones quedan declaradas pero nunca se ejecutan.
@EnableJpaAuditing
@EnableSpringDataWebSupport(
    pageSerializationMode =
        EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO
)
public class SvuabackApplication {

	public static void main(String[] args) {
		SpringApplication.run(SvuabackApplication.class, args);
	}

}
