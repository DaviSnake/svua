package cl.aracridav.svua.shared.service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.empresa.repository.EmpresaRepository;
import cl.aracridav.svua.shared.dto.response.LogArchivoResponse;
import cl.aracridav.svua.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;

/**
 * Lista y sirve el contenido de los archivos .txt de error generados por
 * ImportFileLogService (uno por ejecucion de carga masiva con al menos un
 * error), guardados en log/{empresaId}_{nombreEmpresa}/. Usado solo por
 * la pantalla "Ver logs" (SUPER_ADMIN).
 *
 * 🔥 La carpeta de cada empresa se llama "{empresaId}_{nombreEmpresa}"
 * (ej. "2_Empresa_demo_Spa"), no solo el id — asi se identifica a
 * simple vista en el filesystem. Como el nombre puede cambiar si la
 * empresa se renombra despues de generarse logs viejos, la busqueda de
 * carpetas SIEMPRE es por el prefijo "{empresaId}_" (el id es estable),
 * nunca por el nombre completo de la carpeta. Se mantiene compatibilidad
 * con carpetas antiguas creadas antes de este cambio, que se llamaban
 * solo con el id (sin sufijo).
 */
@Service
@RequiredArgsConstructor
public class LogArchivoServiceImpl implements LogArchivoService {

    private static final Path DIRECTORIO_BASE = Paths.get("log");

    private final EmpresaRepository empresaRepository;

    @Override
    public Page<LogArchivoResponse> listar(Long empresaId, Pageable pageable) {

        List<Path> carpetasEmpresa;

        if (empresaId != null) {
            carpetasEmpresa = carpetasDeEmpresa(empresaId);
        } else {
            carpetasEmpresa = todasLasCarpetas();
        }

        Map<Long, String> nombresEmpresa = new HashMap<>();
        List<LogArchivoResponse> todos = new ArrayList<>();

        for (Path carpeta : carpetasEmpresa) {

            Long idEmpresaCarpeta = empresaId != null ? empresaId : parseIdCarpeta(carpeta);

            if (idEmpresaCarpeta == null) {
                continue;
            }

            String nombreEmpresa = nombresEmpresa.computeIfAbsent(idEmpresaCarpeta, id ->
                empresaRepository.findById(id)
                    .map(Empresa::getNombre)
                    .orElse("empresa-" + id)
            );

            try (DirectoryStream<Path> archivos = Files.newDirectoryStream(carpeta, "*.txt")) {
                for (Path archivo : archivos) {
                    todos.add(LogArchivoResponse.builder()
                        .empresaId(idEmpresaCarpeta)
                        .nombreEmpresa(nombreEmpresa)
                        .nombreArchivo(archivo.getFileName().toString())
                        .tamanioBytes(Files.size(archivo))
                        .fechaCreacion(
                            LocalDateTime.ofInstant(
                                Files.getLastModifiedTime(archivo).toInstant(),
                                ZoneId.systemDefault()))
                        .build());
                }
            } catch (IOException e) {
                throw new BusinessException("No se pudo leer los logs de la empresa " + idEmpresaCarpeta, e);
            }
        }

        todos.sort(Comparator.comparing(LogArchivoResponse::getFechaCreacion).reversed());

        int total = todos.size();
        int desde = (int) pageable.getOffset();

        if (desde >= total) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        int hasta = Math.min(desde + pageable.getPageSize(), total);

        return new PageImpl<>(todos.subList(desde, hasta), pageable, total);
    }

    @Override
    public String leerContenido(Long empresaId, String nombreArchivo) {

        // 🔒 el nombre de archivo viene de un @RequestParam: se valida que
        // no intente escapar de la carpeta de la empresa (path traversal).
        if (nombreArchivo == null
                || nombreArchivo.isBlank()
                || nombreArchivo.contains("..")
                || nombreArchivo.contains("/")
                || nombreArchivo.contains("\\")) {
            throw new BusinessException("Nombre de archivo invalido");
        }

        Path carpetaEmpresa = null;

        for (Path candidata : carpetasDeEmpresa(empresaId)) {
            if (Files.isRegularFile(candidata.resolve(nombreArchivo))) {
                carpetaEmpresa = candidata.normalize();
                break;
            }
        }

        if (carpetaEmpresa == null) {
            throw new BusinessException("El archivo de log no existe");
        }

        Path archivo = carpetaEmpresa.resolve(nombreArchivo).normalize();

        if (!archivo.startsWith(carpetaEmpresa) || !Files.isRegularFile(archivo)) {
            throw new BusinessException("El archivo de log no existe");
        }

        try {
            return String.join("\n", Files.readAllLines(archivo));
        } catch (IOException e) {
            throw new BusinessException("No se pudo leer el archivo de log", e);
        }
    }

    // 🔥 Todas las carpetas de log/ que correspondan a una empresa puntual:
    // matchea por el prefijo "{empresaId}_" (carpeta actual, con nombre)
    // y, por compatibilidad, tambien la carpeta antigua sin sufijo (solo
    // el id). Puede haber mas de una si la empresa fue renombrada entre
    // distintas cargas.
    private List<Path> carpetasDeEmpresa(Long empresaId) {

        List<Path> resultado = new ArrayList<>();

        if (!Files.isDirectory(DIRECTORIO_BASE) || empresaId == null) {
            return resultado;
        }

        String prefijo = empresaId + "_";
        String nombreAntiguo = String.valueOf(empresaId);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(DIRECTORIO_BASE)) {
            for (Path p : stream) {
                if (!Files.isDirectory(p)) {
                    continue;
                }
                String nombre = p.getFileName().toString();
                if (nombre.startsWith(prefijo) || nombre.equals(nombreAntiguo)) {
                    resultado.add(p);
                }
            }
        } catch (IOException e) {
            throw new BusinessException("No se pudo leer el directorio de logs", e);
        }

        return resultado;
    }

    private List<Path> todasLasCarpetas() {

        List<Path> resultado = new ArrayList<>();

        if (!Files.isDirectory(DIRECTORIO_BASE)) {
            return resultado;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(DIRECTORIO_BASE)) {
            for (Path p : stream) {
                if (Files.isDirectory(p)) {
                    resultado.add(p);
                }
            }
        } catch (IOException e) {
            throw new BusinessException("No se pudo leer el directorio de logs", e);
        }

        return resultado;
    }

    // Extrae el id de empresa desde el nombre de la carpeta: "2_Empresa_demo"
    // -> 2, o "2" (carpeta antigua sin sufijo) -> 2.
    private Long parseIdCarpeta(Path carpeta) {
        String nombre = carpeta.getFileName().toString();
        int guionBajo = nombre.indexOf('_');
        String idParte = guionBajo == -1 ? nombre : nombre.substring(0, guionBajo);
        try {
            return Long.valueOf(idParte);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
