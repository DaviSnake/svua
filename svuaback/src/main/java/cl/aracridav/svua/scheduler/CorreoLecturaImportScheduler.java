package cl.aracridav.svua.scheduler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import cl.aracridav.svua.controlturno.service.CorreoLecturaImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class CorreoLecturaImportScheduler {

    private final CorreoLecturaImportService correoLecturaImportService;

    @Value("${svua.scheduler.correo-lecturas.enabled}")
    private boolean habilitado;

    @Scheduled(fixedRate = 600000) // 10 minutos
    public void ejecutar() {

        if (!habilitado) {
            return;
        }

        try {
            correoLecturaImportService.revisarBandejaEImportar();
        } catch (Exception ex) {
            // 🔐 un fallo de conexion/formato no debe tumbar el
            // scheduler completo: se reintenta solo en el proximo ciclo.
            log.error("Error al importar lecturas de Control de Turno desde el correo", ex);
        }
    }
}
