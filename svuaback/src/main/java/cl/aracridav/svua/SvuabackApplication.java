package cl.aracridav.svua;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy
@SpringBootApplication
public class SvuabackApplication {

	public static void main(String[] args) {
		SpringApplication.run(SvuabackApplication.class, args);
	}

}
