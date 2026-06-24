package cl.aracridav.svua.scheduler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import cl.aracridav.svua.inventario.activo.entity.Activo;
import cl.aracridav.svua.inventario.activo.repository.ActivoRepository;
import cl.aracridav.svua.inventario.historial.repository.HistorialEstadoActivoRepository;
import cl.aracridav.svua.mantenimiento.orden.entity.EstadoOrden;
import cl.aracridav.svua.mantenimiento.orden.entity.OrdenMantenimiento;
import cl.aracridav.svua.mantenimiento.orden.entity.TipoMantenimiento;
import cl.aracridav.svua.mantenimiento.orden.repository.OrdenMantenimientoRepository;
import cl.aracridav.svua.mantenimiento.orden.repository.OrdenReprogramacionRepository;
import cl.aracridav.svua.mantenimiento.ordenrepuesto.repository.OrdenRepuestoRepository;
import cl.aracridav.svua.mantenimiento.repuesto.entity.Repuesto;
import cl.aracridav.svua.mantenimiento.repuesto.repository.RepuestoRepository;
import cl.aracridav.svua.notificacion.repository.NotificacionRepository;
import cl.aracridav.svua.shared.enums.EstadoActivo;
import cl.aracridav.svua.shared.service.EmailService;
import cl.aracridav.svua.usuario.entity.SesionUsuario;
import cl.aracridav.svua.usuario.repository.SesionUsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class MantencionScheduler {

    private final OrdenMantenimientoRepository ordenRepository;
    private final EmailService emailService;
    private final SesionUsuarioRepository sesionRepository;

    private final NotificacionRepository notificacionRepository;
    private final OrdenRepuestoRepository ordenRepuestoRepository;
    private final OrdenReprogramacionRepository ordenReprogramacionRepository;
    private final HistorialEstadoActivoRepository historialEstadoActivoRepository;
    private final OrdenMantenimientoRepository ordenMantenimientoRepository;
    private final RepuestoRepository repuestoRepository;
    private final ActivoRepository activoRepository;

    @Value("${svua.scheduler.mantenciones.enabled}")
    private boolean enabled;

    @Value("${svua.scheduler.mantenciones.diasnotificacion}")
    private int diasNotificaion;

    @Value("${app.demo.empresa-id}")
    private Long empresaId;

    @Transactional
    @Scheduled(cron = "${app.jobs.ordenes.cron}")
    public void generarNotificacionesMantenciones() {

        if (!enabled) {
            return;
        }

        LocalDate fechaNotificacion = LocalDate.now().plusDays(diasNotificaion);
        LocalDateTime desde = fechaNotificacion.atStartOfDay();
        LocalDateTime hasta = fechaNotificacion.atTime(23, 59, 59);

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

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cerrarSesionesInactivas() {

        LocalDateTime limite =
            LocalDateTime.now()
                .minusMinutes(2);

        List<SesionUsuario> sesiones =
            sesionRepository
                .findByActivaTrueAndUltimaActividadBefore(
                    limite
                );

        sesiones.forEach(sesion -> {

            sesion.setActiva(false);

            sesion.setFechaLogout(
                LocalDateTime.now()
            );

            log.info(
                "Sesión cerrada por inactividad: {}",
                sesion.getUsuario()
                    .getNombre()
            );

        });
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void eliminarOrdenesEmpresaDemo() {

        log.info("Iniciando limpieza de órdenes de la empresa {}", empresaId);

        // Eliminar órdenes
        notificacionRepository.deleteByEmpresaId(empresaId);
        ordenRepuestoRepository.deleteByEmpresaId(empresaId);
        ordenReprogramacionRepository.deleteByEmpresaId(empresaId);
        historialEstadoActivoRepository.deleteByEmpresaIdAndComentarioNot(empresaId);
        ordenMantenimientoRepository.deleteByEmpresaId(empresaId);

        // Restaurar stock
        List<Repuesto> repuestos =
                repuestoRepository.findByEmpresaId(empresaId);

        repuestos.forEach(r -> r.setStockActual(15));

        repuestoRepository.saveAll(repuestos);

        // Restaurar estado activo
        List<Activo> activos =
                activoRepository.findByEmpresaId(empresaId);

        activos.forEach(r -> r.setEstadoActual(EstadoActivo.OPERATIVO));

        activoRepository.saveAll(activos);



        log.info("Limpieza finalizada correctamente");

    }

}
