package cl.aracridav.svua;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@EnableAspectJAutoProxy
@SpringBootApplication
@EnableSpringDataWebSupport(
    pageSerializationMode =
        EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO
)
public class SvuabackApplication {

	public static void main(String[] args) {
		SpringApplication.run(SvuabackApplication.class, args);
	}

}
