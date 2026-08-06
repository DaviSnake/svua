package cl.aracridav.svua.shared.dto.response;

import lombok.Data;

@Data
public class ErrorProceso {

    private int fila;
    private String mensaje;

    public ErrorProceso(int fila, String mensaje) {
        this.fila = fila;
        this.mensaje = mensaje;
    }

}
