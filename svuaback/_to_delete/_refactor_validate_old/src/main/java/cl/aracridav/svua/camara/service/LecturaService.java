package cl.aracridav.svua.camara.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import cl.aracridav.svua.camara.entity.LecturaTemperatura;
import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.empresa.repository.EmpresaRepository;
import cl.aracridav.svua.camara.repository.LecturaRepository;
import cl.aracridav.svua.shared.exception.BusinessException;
import cl.aracridav.svua.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LecturaService {
    private static final long EMPRESA_ID_DEFAULT = 1L;

    private final LecturaRepository lecturaRepository;
    private final EmpresaRepository empresaRepository;
    private final OcrService ocrService;

    @Transactional
    public LecturaTemperatura procesarYGuardar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) throw new BusinessException("Debe enviar una imagen");
        try (InputStream in = archivo.getInputStream()) {
            BufferedImage imagen = ImageIO.read(in);

            if (imagen == null) throw new BusinessException("El archivo enviado no es una imagen válida");

            OcrService.ResultadoOcr resultado = ocrService.procesar(imagen);
            Empresa empresa = empresaRepository.findById(EMPRESA_ID_DEFAULT)
                    .orElseThrow(() -> new BusinessException("Empresa no encontrada"));
            return lecturaRepository.save(LecturaTemperatura.builder().temperatura(resultado.temperatura())
                    .textoOcr(resultado.texto()).fechaLectura(LocalDateTime.now()).empresa(empresa).build());
        } catch (IOException ex) {
            throw new BusinessException("No fue posible leer la imagen", ex);
        }
    }

    @Transactional(readOnly = true)
    public List<LecturaTemperatura> listar() {
        return lecturaRepository.findByEmpresaIdOrderByFechaLecturaDesc(SecurityUtils.getEmpresaId());
    }

    public void guardarImagen(MultipartFile imagen) throws IOException {
        String nombre = "captura_" + System.currentTimeMillis() + ".jpg";
        Path ruta = Paths.get("capturas");

        if (!Files.exists(ruta)) {
            Files.createDirectories(ruta);
        }

        Files.write(ruta.resolve(nombre), imagen.getBytes());
        System.out.println("Imagen guardada: " + nombre);
    }
}
