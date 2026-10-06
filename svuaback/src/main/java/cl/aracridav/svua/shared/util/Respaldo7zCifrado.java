package cl.aracridav.svua.shared.util;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.sf.sevenzipjbinding.ICryptoGetTextPassword;
import net.sf.sevenzipjbinding.IOutCreateArchive7z;
import net.sf.sevenzipjbinding.IOutCreateCallback;
import net.sf.sevenzipjbinding.IOutItem7z;
import net.sf.sevenzipjbinding.ISequentialInStream;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.OutItemFactory;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.impl.RandomAccessFileOutStream;

// Arma un .7z con clave y cabeceras cifradas: 7-Zip/WinRAR piden la clave
// UNA vez al abrir el archivo (con un .zip la lista de nombres queda a la
// vista y la clave se pide archivo por archivo).
public final class Respaldo7zCifrado {

    public record Entrada(String nombre, byte[] contenido, Path archivo) {

        public static Entrada deBytes(String nombre, byte[] contenido) {
            return new Entrada(nombre, contenido, null);
        }

        public static Entrada deArchivo(String nombre, Path archivo) {
            return new Entrada(nombre, null, archivo);
        }

        private long tamano() throws IOException {
            return contenido != null ? contenido.length : Files.size(archivo);
        }
    }

    private static boolean nativoInicializado = false;

    private Respaldo7zCifrado() {
    }

    public static byte[] crear(List<Entrada> entradas, char[] clave) throws IOException {

        inicializarNativo();

        Path temporal = Files.createTempFile("respaldo-", ".7z");
        Callback callback = new Callback(entradas, new String(clave));

        try {
            try (RandomAccessFile raf = new RandomAccessFile(temporal.toFile(), "rw");
                 IOutCreateArchive7z archivo = SevenZip.openOutArchive7z()) {

                archivo.setLevel(5);
                archivo.setHeaderEncryption(true);
                archivo.createArchive(new RandomAccessFileOutStream(raf), entradas.size(), callback);

            } catch (SevenZipException ex) {
                throw new IOException("No fue posible generar el archivo 7z", ex);
            }

            return Files.readAllBytes(temporal);

        } finally {
            callback.cerrarArchivosAbiertos();
            Files.deleteIfExists(temporal);
        }
    }

    private static synchronized void inicializarNativo() throws IOException {
        if (nativoInicializado) {
            return;
        }
        try {
            SevenZip.initSevenZipFromPlatformJAR();
            nativoInicializado = true;
        } catch (Exception ex) {
            throw new IOException("No fue posible inicializar la libreria 7-Zip", ex);
        }
    }

    private static final class Callback implements IOutCreateCallback<IOutItem7z>, ICryptoGetTextPassword {

        private final List<Entrada> entradas;
        private final String clave;
        private final List<RandomAccessFile> abiertos = new ArrayList<>();

        Callback(List<Entrada> entradas, String clave) {
            this.entradas = entradas;
            this.clave = clave;
        }

        @Override
        public void setTotal(long total) {
        }

        @Override
        public void setCompleted(long completado) {
        }

        @Override
        public void setOperationResult(boolean operacionExitosa) {
        }

        @Override
        public IOutItem7z getItemInformation(int indice, OutItemFactory<IOutItem7z> fabrica)
                throws SevenZipException {
            Entrada entrada = entradas.get(indice);
            IOutItem7z item = fabrica.createOutItem();
            try {
                item.setDataSize(entrada.tamano());
            } catch (IOException ex) {
                throw new SevenZipException("No se pudo leer el tamaño de " + entrada.nombre(), ex);
            }
            item.setPropertyPath(entrada.nombre());
            return item;
        }

        @Override
        public ISequentialInStream getStream(int indice) throws SevenZipException {
            Entrada entrada = entradas.get(indice);
            if (entrada.contenido() != null) {
                return new FlujoDeBytes(entrada.contenido());
            }
            try {
                RandomAccessFile raf = new RandomAccessFile(entrada.archivo().toFile(), "r");
                abiertos.add(raf);
                return new RandomAccessFileInStream(raf);
            } catch (IOException ex) {
                throw new SevenZipException("No se pudo abrir " + entrada.nombre(), ex);
            }
        }

        @Override
        public String cryptoGetTextPassword() {
            return clave;
        }

        void cerrarArchivosAbiertos() {
            for (RandomAccessFile raf : abiertos) {
                try {
                    raf.close();
                } catch (IOException ignorada) {
                    // ya se genero (o fallo) el archivo; no hay nada mas que hacer
                }
            }
        }
    }

    private static final class FlujoDeBytes implements ISequentialInStream {

        private final byte[] datos;
        private int posicion = 0;

        FlujoDeBytes(byte[] datos) {
            this.datos = datos;
        }

        @Override
        public int read(byte[] destino) {
            int n = Math.min(destino.length, datos.length - posicion);
            if (n <= 0) {
                return 0;
            }
            System.arraycopy(datos, posicion, destino, 0, n);
            posicion += n;
            return n;
        }

        @Override
        public void close() {
        }
    }
}
