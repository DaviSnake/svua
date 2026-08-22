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
 * error), guardados en log/{empresaId}/. Usado solo por la pantalla
 * "Ver logs" (SUPER_ADMIN).
 */
@Service
@RequiredArgsConstructor
public class LogArchivoServiceImpl implements LogArchivoService {

    private static final Path DIRECTORIO_BASE = Paths.get("log");

    private final EmpresaRepository empresaRepository;

    @Override
    public Page<LogArchivoResponse> listar(Long empresaId, Pageable pageable) {

        List<Path> carpetasEmpresa = new ArrayList<>();

        if (!Files.isDirectory(DIRECTORIO_BASE)) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        if (empresaId != null) {
            Path carpeta = DIRECTORIO_BASE.resolve(String.valueOf(empresaId));
            if (Files.isDirectory(carpeta)) {
                carpetasEmpresa.add(carpeta);
            }
        } else {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(DIRECTORIO_BASE)) {
                for (Path p : stream) {
                    if (Files.isDirectory(p)) {
                        carpetasEmpresa.add(p);
                    }
                }
            } catch (IOException e) {
                throw new BusinessException("No se pudo leer el directorio de logs", e);
            }
        }

        Map<Long, String> nombresEmpresa = new HashMap<>();
        List<LogArchivoResponse> todos = new ArrayList<>();

        for (Path carpeta : carpetasEmpresa) {

            Long idEmpresaCarpeta = parseIdCarpeta(carpeta);

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

        Path carpetaEmpresa = DIRECTORIO_BASE.resolve(String.valueOf(empresaId)).normalize();
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

    private Long parseIdCarpeta(Path carpeta) {
        try {
            return Long.valueOf(carpeta.getFileName().toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
