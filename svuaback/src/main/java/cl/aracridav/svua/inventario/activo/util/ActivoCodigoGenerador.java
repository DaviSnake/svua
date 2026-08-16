package cl.aracridav.svua.inventario.activo.util;

/**
 * Genera el codigo QR y el codigo EAN13 que se guardan en el activo al
 * momento de crearlo (o de renombrar su codigo interno). Solo se genera el
 * contenido/texto de cada codigo, no una imagen: el QR y el codigo de
 * barras se dibujan en el frontend a partir de este texto cuando se
 * necesiten mostrar o imprimir.
 */
public final class ActivoCodigoGenerador {

    private ActivoCodigoGenerador() {
    }

    // El QR codifica unicamente el codigo interno del activo (texto plano),
    // sin URL ni datos adicionales.
    public static String generarCodigoQr(String codigoInterno) {
        return codigoInterno;
    }

    // El EAN13 exige exactamente 13 digitos numericos (12 + digito
    // verificador). Como el codigo interno puede tener letras u otros
    // caracteres, se deriva un numero de 12 digitos a partir de su hash y
    // se le agrega el digito verificador estandar de EAN13.
    public static String generarCodigoEan13(String codigoInterno) {
        long hash = Math.abs((long) codigoInterno.hashCode());
        String base12 = String.format("%012d", hash % 1_000_000_000_000L);
        int digitoVerificador = calcularDigitoVerificadorEan13(base12);
        return base12 + digitoVerificador;
    }

    private static int calcularDigitoVerificadorEan13(String base12) {
        int suma = 0;
        for (int i = 0; i < base12.length(); i++) {
            int digito = base12.charAt(i) - '0';
            // Posiciones impares (indice par, empezando en 0) pesan 1;
            // posiciones pares (indice impar) pesan 3 - estandar EAN13.
            suma += (i % 2 == 0) ? digito : digito * 3;
        }
        int resto = suma % 10;
        return resto == 0 ? 0 : 10 - resto;
    }
}
