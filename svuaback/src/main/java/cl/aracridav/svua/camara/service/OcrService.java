package cl.aracridav.svua.camara.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import cl.aracridav.svua.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OcrService {
    private static final Pattern TEMPERATURA = Pattern.compile("(\\d+\\.\\d+)");
    private final OpenCvService openCvService;
    private final TesseractService tesseractService;

    public ResultadoOcr procesar(BufferedImage imagen) throws IOException {
        String texto = tesseractService.leerTexto(openCvService.prepararParaOcr(imagen));
        Matcher matcher = TEMPERATURA.matcher(texto);
        if (!matcher.find()) throw new BusinessException("No se encontró una temperatura en la imagen: " + texto);
        BigDecimal temperatura = new BigDecimal(matcher.group(1).replace(',', '.'));
        if (temperatura.compareTo(new BigDecimal("-100")) < 0 || temperatura.compareTo(new BigDecimal("200")) > 0) {
            throw new BusinessException("La temperatura detectada está fuera de rango");
        }
        return new ResultadoOcr(temperatura, texto);
    }

    public record ResultadoOcr(BigDecimal temperatura, String texto) {}
}
