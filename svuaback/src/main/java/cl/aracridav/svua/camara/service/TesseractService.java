package cl.aracridav.svua.camara.service;

import java.awt.image.BufferedImage;

import java.io.IOException;

import org.springframework.stereotype.Service;
import cl.aracridav.svua.camara.config.OpenCVConfig;
import cl.aracridav.svua.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

@Service
@RequiredArgsConstructor
public class TesseractService {
    private final OpenCVConfig config;

    public String leerTexto(BufferedImage imagen) throws IOException {
        try {
            Tesseract tesseract = new Tesseract();
            if (config.getTesseractDataPath() != null && !config.getTesseractDataPath().isBlank()) {
                tesseract.setDatapath(config.getTesseractDataPath());
            }
            tesseract.setVariable("user_defined_dpi", "300");
            tesseract.setVariable("tessedit_char_whitelist", "0123456789.-");
            tesseract.setLanguage(config.getLanguage());
            tesseract.setPageSegMode(7);
            String texto = tesseract.doOCR(imagen);
            System.out.println("OCR=[" + texto + "]");
            return texto;
        } catch (TesseractException ex) {
            throw new BusinessException("No fue posible ejecutar OCR. Configure ocr.tesseract.data-path", ex);
        }
    }
}
