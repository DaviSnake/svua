package cl.aracridav.svua.camara.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Service;
import cl.aracridav.svua.shared.exception.BusinessException;

@Service
public class OpenCvService {
    /*public BufferedImage prepararParaOcr(BufferedImage original) {
        if (original == null) throw new BusinessException("La imagen no es válida");
        int ancho = original.getWidth() * 2;
        int alto = original.getHeight() * 2;
        BufferedImage resultado = new BufferedImage(ancho, alto, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = resultado.createGraphics();
        graphics.drawImage(original, 0, 0, ancho, alto, null);
        graphics.dispose();

        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                int gris = resultado.getRaster().getSample(x, y, 0);
                resultado.getRaster().setSample(x, y, 0, gris > 100 ? 255 : 0);
            }
        }
        return resultado;
    }

    public BufferedImage prepararParaOcr(BufferedImage original) throws IOException {

        if (original == null) {
            throw new BusinessException("La imagen no es válida");
        }

        // SOLO LA TEMPERATURA
        int x = 250;
        int y = 155;
        int ancho = 180;
        int alto = 90;

        BufferedImage recorte = original.getSubimage(x, y, ancho, alto);

        int nuevoAncho = ancho * 4;
        int nuevoAlto = alto * 4;

        BufferedImage resultado = new BufferedImage(
                nuevoAncho,
                nuevoAlto,
                BufferedImage.TYPE_BYTE_GRAY);

        Graphics2D g = resultado.createGraphics();
        g.drawImage(recorte, 0, 0, nuevoAncho, nuevoAlto, null);
        g.dispose();

        ImageIO.write(recorte, "png", new File("C:/temp/recorte.png"));

        /*for (int yy = 0; yy < nuevoAlto; yy++) {
            for (int xx = 0; xx < nuevoAncho; xx++) {

                int gris = resultado.getRaster().getSample(xx, yy, 0);

                resultado.getRaster().setSample(
                        xx,
                        yy,
                        0,
                        gris > 120 ? 255 : 0);
            }
        }

        ImageIO.write(resultado, "png", new File("C:/temp/binaria.png"));

         // ===== GUARDAR IMAGEN PARA REVISAR =====
        try {
            File carpeta = new File("C:/temp");
            if (!carpeta.exists()) {
                carpeta.mkdirs();
            }

            ImageIO.write(resultado, "png", new File(carpeta, "temperatura.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return resultado;
    }*/

    public BufferedImage prepararParaOcr(BufferedImage original) throws IOException {

        if (original == null) {
            throw new BusinessException("La imagen no es válida");
        }

        // ==========================
        // BufferedImage -> Mat
        // ==========================
        Mat mat = bufferedImageToMat(original);

        // ==========================
        // RECORTE
        // Ajustar si es necesario
        // ==========================
        Rect roi = new Rect(250, 155, 180, 90);

        Mat recorte = new Mat(mat, roi);

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
