package cl.aracridav.svua.camara.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import lombok.Getter;

@Configuration
@Getter
public class OpenCVConfig {
    @Value("${ocr.tesseract.data-path}")
    private String tesseractDataPath;

    @Value("${ocr.tesseract.language}")
    private String language;

    @PostConstruct
    public void init() {
        nu.pattern.OpenCV.loadLocally();
    }
}
