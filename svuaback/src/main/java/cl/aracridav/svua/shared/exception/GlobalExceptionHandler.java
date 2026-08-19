package cl.aracridav.svua.shared.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(
            BusinessException ex) {

        Map<String, Object> body = new HashMap<>();
        body.put("error", ex.getMessage());
        body.put("timestamp", LocalDateTime.now());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(body);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<?> handleInvalidRefreshToken(
            InvalidRefreshTokenException ex) {

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                        "error", "INVALID_REFRESH_TOKEN",
                        "message", ex.getMessage()
                ));
    }

    // 🔐 Lanzada por Hibernate/Spring Data cuando un save() pisa una
    // fila cuya columna @Version ya cambió por otra transacción
    // concurrente (ver Repuesto.stockActual). Sin este handler caía
    // como 500 genérico de Spring en vez de un error de negocio claro.
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleOptimisticLock(
            ObjectOptimisticLockingFailureException ex) {

        Map<String, Object> body = new HashMap<>();
        body.put("error", "El stock fue modificado por otra operación, intente nuevamente");
        body.put("timestamp", LocalDateTime.now());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(body);
    }

}
