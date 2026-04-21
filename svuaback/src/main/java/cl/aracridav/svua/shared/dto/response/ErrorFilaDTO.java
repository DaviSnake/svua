package cl.aracridav.svua.shared.dto.response;

import lombok.Data;

@Data
public class ErrorFilaDTO {
    private int fila;
    private String mensaje;
    private String contenido;

    public ErrorFilaDTO(int fila, String mensaje, String contenido) {
        this.fila = fila;
        this.mensaje = mensaje;
        this.contenido = contenido;
    }

}
