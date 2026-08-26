package cl.aracridav.svua.config.security;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 🔐 Rate limiter en memoria (ConcurrentHashMap) para los endpoints de
 * auth mas sensibles a fuerza bruta: login, request-reset y
 * reset-password. Antes no existia NINGUN limite de intentos por IP en
 * estos endpoints (el bloqueo de AppConstants.MAX_INTENTOS en
 * AuthController es por USUARIO, no por IP, y no cubre request-reset ni
 * reset-password).
 *
 * CAVEAT: solo funciona correctamente con una unica instancia del
 * backend. Si se despliega detras de un load balancer con mas de una
 * replica sin estado compartido (Redis, etc.), cada instancia lleva su
 * propio contador y el limite efectivo se multiplica por el numero de
 * instancias. Para este proyecto (instancia unica) es una mitigacion
 * inmediata aceptable; una solucion definitiva para multiples replicas
 * requeriria un almacen compartido (ej. Bucket4j + Redis).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_INTENTOS = 5;
    private static final long VENTANA_MS = 15 * 60 * 1000L; // 15 minutos

    // 🔥 Solo hora:minuto -- alcanza para lo que necesita ver el usuario
    // ("intente nuevamente despues de las 14:35"), sin acoplar el
    // mensaje a un formato de fecha completo.
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private static class Contador {
        final AtomicInteger intentos = new AtomicInteger(0);
        final AtomicLong inicioVentana;

        Contador(long inicioVentana) {
            this.inicioVentana = new AtomicLong(inicioVentana);
        }
    }

    private final ConcurrentHashMap<String, Contador> intentosPorClave = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return !(path.equals("/api/v1/svua/auth/login")
                || path.equals("/api/v1/svua/auth/request-reset")
                || path.equals("/api/v1/svua/auth/reset-password"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain chain) throws ServletException, IOException {

        String ip = obtenerIp(request);
        String clave = ip + ":" + request.getServletPath();

        long ahora = Instant.now().toEpochMilli();

        Contador contador = intentosPorClave.compute(clave, (k, c) -> {
            if (c == null || ahora - c.inicioVentana.get() > VENTANA_MS) {
                return new Contador(ahora);
            }
            return c;
        });

        int intentos = contador.intentos.incrementAndGet();

        if (intentos > MAX_INTENTOS) {

            // 🔥 La ventana NO es deslizante (ver compute() mas arriba):
            // se desbloquea exactamente VENTANA_MS despues del PRIMER
            // intento contado en esta ventana, no del ultimo.
            LocalDateTime desbloqueo = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(contador.inicioVentana.get() + VENTANA_MS),
                    ZoneId.systemDefault());

            response.setStatus(429); // Too Many Requests
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                "{\"error\":\"Demasiados intentos. Intente nuevamente después de las "
                    + desbloqueo.format(FORMATO_HORA)
                    + ".\",\"reintentarDespuesDe\":\"" + desbloqueo + "\"}"
            );
            return;
        }

        chain.doFilter(request, response);
    }

    // 🔐 Limpieza periodica para evitar que el mapa crezca indefinidamente
    // con IPs/rutas viejas cuya ventana ya expiro.
    @Scheduled(fixedRate = 30 * 60 * 1000) // cada 30 min
    void limpiarContadoresExpirados() {
        long ahora = Instant.now().toEpochMilli();
        intentosPorClave.entrySet().removeIf(
            e -> ahora - e.getValue().inicioVentana.get() > VENTANA_MS
        );
    }

    private String obtenerIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            return ip.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
