package cl.aracridav.svua.scheduler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import cl.aracridav.svua.mantenimiento.orden.entity.EstadoOrden;
import cl.aracridav.svua.mantenimiento.orden.entity.OrdenMantenimiento;
import cl.aracridav.svua.mantenimiento.orden.repository.OrdenMantenimientoRepository;
import cl.aracridav.svua.mantenimiento.plan.entity.TipoMantenimiento;
import cl.aracridav.svua.shared.service.EmailService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MantencionScheduler {

    private final OrdenMantenimientoRepository ordenRepository;
    private final EmailService emailService;

    @Value("${svua.scheduler.mantenciones.enabled}")
    private boolean enabled;

    @Transactional
    @Scheduled(cron = "*/30 * * * * *")
    public void generarNotificacionesMantenciones() {

        if (!enabled) {
            return;
        }

        LocalDate manana = LocalDate.now().plusDays(1);
        LocalDateTime desde = manana.atStartOfDay();
        LocalDateTime hasta = manana.atTime(23, 59, 59);

        List<OrdenMantenimiento> ordenes =
        ordenRepository
            .findByEstadoAndTipoMantenimientoAndFechaProgramadaBetween(
                EstadoOrden.PROGRAMADA,
                TipoMantenimiento.PREVENTIVO,
                desde,
                hasta
            );

        for (OrdenMantenimiento orden : ordenes) {

            System.out.println("ID: " + orden.getId());
            System.out.println("Título: " + orden.getTitulo());
            System.out.println("Email: " + orden.getProveedor().getEmail());

            emailService.sendEmailOrdenProgramada(orden.getProveedor().getEmail(), orden);

        }

    }

}
