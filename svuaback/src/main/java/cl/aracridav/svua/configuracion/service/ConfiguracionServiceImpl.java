package cl.aracridav.svua.configuracion.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import cl.aracridav.svua.configuracion.dto.response.ConfiguracionEntryResponse;
import cl.aracridav.svua.shared.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

// 🔒 Lee/edita el .env real que usa docker-compose para levantar los
// contenedores (ver volumen montado en docker-compose.yml). Pensado
// SOLO para SUPER_ADMIN (el controller restringe el acceso).
//
// IMPORTANTE: Spring Boot solo lee estas variables al arrancar (vía
// @Value / application.properties), así que un cambio guardado acá NO
// se aplica al proceso en ejecución hasta reiniciar el backend.
@Service
@Slf4j
public class ConfiguracionServiceImpl implements ConfiguracionService {

    @Value("${app.config.env-file:.env}")
    private String envFilePath;

    @Override
    public List<ConfiguracionEntryResponse> leerConfiguracion() {

        List<String> lineas = leerLineas();

        List<ConfiguracionEntryResponse> resultado = new ArrayList<>();

        for (String linea : lineas) {

            String[] par = parsearLinea(linea);

            if (par != null) {
                resultado.add(new ConfiguracionEntryResponse(par[0], par[1]));
            }
        }

        return resultado;
    }

    @Override
    public void actualizarConfiguracion(Map<String, String> valores) {

        if (valores == null || valores.isEmpty()) {
            return;
        }

        // 🔒 Evita que un valor con salto de línea inyecte líneas nuevas
        // (y por lo tanto variables nuevas) en el .env.
        for (String valor : valores.values()) {

            if (valor != null && (valor.contains("\n") || valor.contains("\r"))) {
                throw new BusinessException(
                    "El valor de una variable no puede contener saltos de línea");
            }
        }

        List<String> lineas = leerLineas();
        List<String> nuevasLineas = new ArrayList<>(lineas.size());

        for (String linea : lineas) {

            String[] par = parsearLinea(linea);

            if (par != null && valores.containsKey(par[0])) {
                nuevasLineas.add(par[0] + "=" + valores.get(par[0]));
            } else {
                nuevasLineas.add(linea);
            }
        }

        escribirLineas(nuevasLineas);

        log.info(
            "Configuración (.env) actualizada. Claves modificadas: {}",
            valores.keySet());
    }

    private List<String> leerLineas() {

        Path path = resolverPath();

        try {
            return Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BusinessException(
                "No se pudo leer el archivo de configuración: " + e.getMessage());
        }
    }

    private void escribirLineas(List<String> lineas) {

        Path path = resolverPath();

        try {

            // Escritura atómica: se escribe primero a un archivo
            // temporal en el mismo directorio y luego se reemplaza el
            // .env real, para no dejarlo a medio escribir si algo falla.
            Path temp = Files.createTempFile(path.getParent(), ".env-", ".tmp");

            Files.write(temp, lineas, StandardCharsets.UTF_8);

            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException e) {
            throw new BusinessException(
                "No se pudo guardar el archivo de configuración: " + e.getMessage());
        }
    }

    private Path resolverPath() {

        Path directo = Paths.get(envFilePath).toAbsolutePath().normalize();

        if (Files.exists(directo)) {
            return directo;
        }

        // Ejecutando localmente (IDE / mvn) con working directory
        // "svuaback": el .env real vive un nivel arriba, junto a
        // docker-compose.yml. En el contenedor Docker esto no aplica
        // porque el .env ya queda montado directamente en "directo".
        Path unNivelArriba = Paths.get("..", envFilePath).toAbsolutePath().normalize();

        if (Files.exists(unNivelArriba)) {
            return unNivelArriba;
        }

        return directo;
    }

    // Ignora líneas vacías y comentarios (#...). Devuelve {clave, valor}
    // o null si la línea no es una asignación KEY=VALUE.
    private String[] parsearLinea(String linea) {

        String trim = linea.trim();

        if (trim.isEmpty() || trim.startsWith("#")) {
            return null;
        }

        int idx = trim.indexOf('=');

        if (idx <= 0) {
            return null;
        }

        String clave = trim.substring(0, idx).trim();
        String valor = trim.substring(idx + 1).trim();

        return new String[] { clave, valor };
    }

}
