package cl.aracridav.svua.camara.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Service;
import cl.aracridav.svua.shared.exception.BusinessException;

@Service
public class OpenCvService {

    // Margen extra alrededor del grupo de digitos detectado, para no
    // cortar el borde de un digito por error de deteccion
    private static final double MARGEN_ROI = 0.15;

    public BufferedImage prepararParaOcr(BufferedImage original) throws IOException {

        if (original == null) {
            throw new BusinessException("La imagen no es válida");
        }

        // ==========================
        // BufferedImage -> Mat
        // ==========================
        Mat mat = bufferedImageToMat(original);

        Mat grisCompleto = new Mat();
        Imgproc.cvtColor(mat, grisCompleto, Imgproc.COLOR_BGR2GRAY);

        // ==========================
        // RECORTE DINAMICO
        // El ROI ya no es un rectangulo fijo: se detecta donde estan
        // los digitos en cada foto, para no depender de que la camara
        // quede siempre en la misma posicion/resolucion exactas.
        // ==========================
        Rect roi = detectarRoiDisplay(grisCompleto);

        Mat recorte = new Mat(mat, roi);

        Imgcodecs.imwrite("C:/temp/temperatura_recorte.png", recorte);

        // ==========================
        // AUMENTAR X4
        // ==========================
        Mat ampliada = new Mat();

        Imgproc.resize(
                recorte,
                ampliada,
                new Size(),
                4,
                4,
                Imgproc.INTER_CUBIC);

        // ==========================
        // ESCALA DE GRISES
        // ==========================
        Mat gris = new Mat();

        Imgproc.cvtColor(
                ampliada,
                gris,
                Imgproc.COLOR_BGR2GRAY);

        // ==========================
        // REDUCIR RUIDO
        // ==========================
        Imgproc.GaussianBlur(
                gris,
                gris,
                new Size(3,3),
                0);

        // ==========================
        // ENFOQUE
        // ==========================
        Mat sharpen = new Mat();

        Mat kernel = new Mat(3,3, CvType.CV_32F);

        kernel.put(0,0,
                0,-1,0,
                -1,5,-1,
                0,-1,0);

        Imgproc.filter2D(
                gris,
                sharpen,
                gris.depth(),
                kernel);

        // ==========================
        // BINARIZACIÓN
        // ==========================
        Mat binary = new Mat();

        Imgproc.adaptiveThreshold(
                sharpen,
                binary,
                255,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY,
                31,
                5);

        // ==========================
        // GUARDAR PARA DEBUG
        // ==========================
        Imgcodecs.imwrite(
                "C:/temp/temperatura.png",
                binary);

        return matToBufferedImage(binary);

    }

    //========================================================
    // DETECCION DINAMICA DEL DISPLAY
    //========================================================

    private Rect detectarRoiDisplay(Mat gris) {

        Mat suavizada = new Mat();
        Imgproc.GaussianBlur(gris, suavizada, new Size(5, 5), 0);

        // Se prueban las dos polaridades porque no se sabe de antemano
        // si el display es LCD (digitos oscuros sobre fondo claro) o
        // LED (digitos claros sobre fondo oscuro)
        Rect candidatoLcd = detectarGrupoDigitos(suavizada, false);
        Rect candidatoLed = detectarGrupoDigitos(suavizada, true);

        Rect elegido = mejorCandidato(candidatoLcd, candidatoLed);

        if (elegido == null) {
            throw new BusinessException(
                    "No se detectó el display en la imagen. Verifique el encuadre, la distancia y la iluminación de la cámara.");
        }

        return agregarMargen(elegido, gris.size());
    }

    private Rect detectarGrupoDigitos(Mat gris, boolean invertir) {

        Mat binaria = new Mat();
        int tipo = invertir
                ? Imgproc.THRESH_BINARY_INV | Imgproc.THRESH_OTSU
                : Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU;
        Imgproc.threshold(gris, binaria, 0, 255, tipo);

        List<MatOfPoint> contornos = new ArrayList<>();
        Imgproc.findContours(binaria, contornos, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        double alturaImagen = gris.rows();
        double alturaMinima = alturaImagen * 0.04;
        double alturaMaxima = alturaImagen * 0.6;

        List<Rect> candidatos = new ArrayList<>();
        for (MatOfPoint contorno : contornos) {
            Rect r = Imgproc.boundingRect(contorno);
            double aspecto = (double) r.height / r.width;
            if (r.height >= alturaMinima && r.height <= alturaMaxima && aspecto >= 1.0 && aspecto <= 4.5) {
                candidatos.add(r);
            }
        }

        // Un solo bloque detectado no es confiable: una temperatura
        // como "23.5" son al menos 2-3 digitos alineados
        if (candidatos.size() < 2) {
            return null;
        }

        return agruparPorFilaDominante(candidatos);
    }

    // Agrupa los rectangulos que estan alineados en la misma fila
    // (misma banda vertical, es decir el mismo renglon de digitos) y
    // devuelve el rectangulo mas grande que los engloba a todos
    private Rect agruparPorFilaDominante(List<Rect> candidatos) {

        candidatos.sort((a, b) -> Integer.compare(centroY(a), centroY(b)));

        List<Rect> mejorGrupo = new ArrayList<>();
        List<Rect> grupoActual = new ArrayList<>();

        for (Rect r : candidatos) {
            if (grupoActual.isEmpty()
                    || Math.abs(centroY(r) - centroGrupo(grupoActual)) < promedioAltura(grupoActual)) {
                grupoActual.add(r);
            } else {
                if (grupoActual.size() > mejorGrupo.size()) {
                    mejorGrupo = new ArrayList<>(grupoActual);
                }
                grupoActual = new ArrayList<>(List.of(r));
            }
        }
        if (grupoActual.size() > mejorGrupo.size()) {
            mejorGrupo = grupoActual;
        }

        if (mejorGrupo.size() < 2) {
            return null;
        }

        int minX = mejorGrupo.stream().mapToInt(r -> r.x).min().orElseThrow();
        int minY = mejorGrupo.stream().mapToInt(r -> r.y).min().orElseThrow();
        int maxX = mejorGrupo.stream().mapToInt(r -> r.x + r.width).max().orElseThrow();
        int maxY = mejorGrupo.stream().mapToInt(r -> r.y + r.height).max().orElseThrow();

        return new Rect(minX, minY, maxX - minX, maxY - minY);
    }

    private int centroY(Rect r) {
        return r.y + r.height / 2;
    }

    private int centroGrupo(List<Rect> grupo) {
        return (int) grupo.stream().mapToInt(this::centroY).average().orElse(0);
    }

    private int promedioAltura(List<Rect> grupo) {
        return (int) grupo.stream().mapToInt(r -> r.height).average().orElse(1);
    }

    // Entre las dos polaridades probadas, se queda con el grupo de
    // mayor area (el que mas probablemente corresponde a los digitos
    // reales y no a ruido de fondo)
    private Rect mejorCandidato(Rect a, Rect b) {
        if (a == null) return b;
        if (b == null) return a;
        return (a.area() >= b.area()) ? a : b;
    }

    private Rect agregarMargen(Rect roi, Size tamanoImagen) {
        int margenX = (int) (roi.width * MARGEN_ROI);
        int margenY = (int) (roi.height * MARGEN_ROI);

        int x = Math.max(0, roi.x - margenX);
        int y = Math.max(0, roi.y - margenY);
        int ancho = Math.min((int) tamanoImagen.width - x, roi.width + margenX * 2);
        int alto = Math.min((int) tamanoImagen.height - y, roi.height + margenY * 2);

        return new Rect(x, y, ancho, alto);
    }

    //========================================================

    private Mat bufferedImageToMat(BufferedImage bi) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            ImageIO.write(bi, "jpg", baos);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Mat mat = Imgcodecs.imdecode(
                new MatOfByte(baos.toByteArray()),
                Imgcodecs.IMREAD_COLOR);

        return mat;
    }

    //========================================================

    private BufferedImage matToBufferedImage(Mat mat) throws IOException {

        MatOfByte mob = new MatOfByte();

        Imgcodecs.imencode(".png", mat, mob);

        return ImageIO.read(
                new ByteArrayInputStream(mob.toArray()));
    }

}
