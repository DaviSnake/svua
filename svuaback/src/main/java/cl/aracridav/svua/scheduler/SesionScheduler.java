package cl.aracridav.svua.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import cl.aracridav.svua.usuario.entity.SesionUsuario;
import cl.aracridav.svua.usuario.repository.SesionUsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Cierra automaticamente las sesiones que quedaron "activa=true" sin
// que el usuario haya cerrado sesion explicitamente (cerro la pestana,
// se corto la conexion, el equipo se suspendio, etc.). Sin este job esas
// sesiones quedarian marcadas como "Online" para siempre, porque el campo
// activa solo se actualiza en un logout explicito (ver cerrarSesion() en
// SesionUsuarioServiceImpl).
@Component
@RequiredArgsConstructor
@Slf4j
public class SesionScheduler {

    // 15 minutos de inactividad (antes eran 2 horas, y 2 minutos antes
    // de eso).
    private static final int MINUTOS_INACTIVIDAD_SESION = 15;

    private final SesionUsuarioRepository sesionRepository;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cerrarSesionesInactivas() {

        LocalDateTime limite =
            LocalDateTime.now().minusMinutes(MINUTOS_INACTIVIDAD_SESION);

        List<SesionUsuario> sesionesInactivas =
            sesionRepository.findByActivaTrueAndUltimaActividadBefore(limite);

        if (sesionesInactivas.isEmpty()) {
            return;
        }

        sesionesInactivas.forEach(sesion -> {
            sesion.setActiva(false);
            sesion.setFechaLogout(LocalDateTime.now());
        });

        sesionRepository.saveAll(sesionesInactivas);

        log.info(
            "Cerradas {} sesiones inactivas (sin actividad hace mas de {} minutos)",
            sesionesInactivas.size(),
            MINUTOS_INACTIVIDAD_SESION);
    }

}
