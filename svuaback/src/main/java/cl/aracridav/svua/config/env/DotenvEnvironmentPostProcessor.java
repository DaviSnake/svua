package cl.aracridav.svua.config.env;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

// Carga el .env de la raiz del repo (el mismo que usa Docker via
// "env_file" en docker-compose.yml) como una fuente de properties de
// Spring, pero con la PRIORIDAD MAS BAJA de todas: si la variable ya
// existe como variable de entorno real del sistema operativo (que es
// exactamente lo que pasa dentro del contenedor Docker, incluyendo en la
// VPS, gracias a "env_file"), esta fuente nunca gana y no cambia nada.
//
// Esto es solo para que correr el backend localmente (IDE / mvn, sin
// Docker) encuentre JWT_SECRET / POSTGRES_PASSWORD / MAIL_PASSWORD igual,
// sin tener que configurarlas a mano en cada maquina de desarrollo — ya
// que application.properties ya no trae valores por defecto hardcodeados
// (se sacaron por seguridad).
//
// La resolucion del archivo (intenta ".env" en el working directory, y
// si no existe prueba un nivel arriba) replica a proposito la misma
// logica ya usada en ConfiguracionServiceImpl.resolverPath() para el
// mismo problema.
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String ENV_FILE_NAME = ".env";

    // Solo se exponen estas claves puntuales, las mismas a las que se les
    // saco el valor por defecto hardcodeado en application.properties por
    // seguridad. El resto del .env (por ejemplo POSTGRES_HOST=postgres,
    // que Docker usa para resolver el nombre del servicio dentro de su
    // red interna) NO se carga: si se cargara completo, correr
    // localmente pisaria el valor localhost por defecto de
    // application.properties y rompería la conexion a la base de datos
    // local.
    private static final Set<String> CLAVES_PERMITIDAS = Set.of(
            "JWT_SECRET",
            "POSTGRES_PASSWORD",
            "MAIL_PASSWORD"
    );

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment,
            SpringApplication application) {

        Path envFile = resolverEnvFile();

        if (envFile == null) {
            return;
        }

        Map<String, Object> valores = leerEnv(envFile);
        valores.keySet().retainAll(CLAVES_PERMITIDAS);

        if (valores.isEmpty()) {
            return;
        }

        // addLast: la prioridad mas baja de todas las fuentes de
        // properties, para que "systemEnvironment" (variables de entorno
        // reales) y "systemProperties" (-D) siempre ganen por sobre esto.
        environment.getPropertySources()
                .addLast(new MapPropertySource("dotenvFile", valores));
    }

    private Path resolverEnvFile() {

        Path directo = Paths.get(ENV_FILE_NAME).toAbsolutePath().normalize();

        if (Files.isRegularFile(directo)) {
            return directo;
        }

        // Ejecutando localmente (IDE / mvn) con working directory
        // "svuaback": el .env real vive un nivel arriba, junto a
        // docker-compose.yml.
        Path unNivelArriba = Paths.get("..", ENV_FILE_NAME).toAbsolutePath().normalize();

        if (Files.isRegularFile(unNivelArriba)) {
            return unNivelArriba;
        }

        return null;
    }

    private Map<String, Object> leerEnv(Path envFile) {

        Map<String, Object> valores = new LinkedHashMap<>();

        List<String> lineas;

        try {
            lineas = Files.readAllLines(envFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            // Si no se puede leer el .env, simplemente no se agrega esta
            // fuente de properties; el arranque sigue su curso normal (y
            // fallara mas adelante, con el mensaje estandar de Spring, si
            // de verdad falta una variable requerida).
            return valores;
        }

        for (String linea : lineas) {

            String trim = linea.trim();

            if (trim.isEmpty() || trim.startsWith("#")) {
                continue;
            }

            int idx = trim.indexOf('=');

            if (idx <= 0) {
                continue;
            }

            String clave = trim.substring(0, idx).trim();
            String valor = trim.substring(idx + 1).trim();

            valores.put(clave, valor);
        }

        return valores;
    }
}
