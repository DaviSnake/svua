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
