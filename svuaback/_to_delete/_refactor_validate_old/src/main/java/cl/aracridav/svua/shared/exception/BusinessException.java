package cl.aracridav.svua.shared.exception;

import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

/**
 * Excepción genérica para reglas de negocio
 */
@ResponseStatus(HttpStatus.BAD_REQUEST) // 400 por defecto
public class BusinessException extends RuntimeException {

    public BusinessException() {
        super();
    }

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }

}
