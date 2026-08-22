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
import cl.aracridav.svua.multitenancy.RlsContextService;
import cl.aracridav.svua.notificacion.repository.NotificacionRepository;
import cl.aracridav.svua.shared.enums.EstadoActivo;
import cl.aracridav.svua.shared.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class MantencionScheduler {

    private static final int STOCK_INICIAL_DEMO = 15;

    private final OrdenMantenimientoRepository ordenRepository;
    private final EmailService emailService;

    private final NotificacionRepository notificacionRepository;
    // 🔒 Row Level Security (ver migracion V27): este scheduler corre
    // fuera de un request HTTP, no hay Authentication en el
    // SecurityContext, asi que EmpresaFilter nunca se ejecuta para el
    // (a diferencia de un metodo de servicio llamado desde un
    // controller). Sin setear esto explicitamente, Postgres devolveria
    // cero filas en TODAS las consultas de estos metodos.
    private final RlsContextService rlsContextService;
    private final OrdenRepuestoRepository ordenRepuestoRepository;
    private final OrdenReprogramacionRepository ordenReprogramacionRepository;
    private final HistorialEstadoActivoRepository historialEstadoActivoRepository;
    private final RepuestoRepository repuestoRepository;
    private final ActivoRepository activoRepository;

    @Value("${svua.scheduler.mantenciones.enabled}")
    private boolean enabled;

    @Value("${svua.scheduler.mantenciones.diasnotificacion}")
    private int diasNotificacion;

    @Value("${app.demo.empresa-id}")
    private Long empresaId;

    @Value("${svua.scheduler.limpiezademo.enabled:false}")
    private boolean limpiezaDemoEnabled;

    @Transactional
    @Scheduled(cron = "${app.jobs.ordenes.cron}")
    public void generarNotificacionesMantenciones() {

        if (!enabled) {
            return;
        }

        // 🔒 este job recorre ordenes PROGRAMADA de TODAS las empresas
        // (no de una sola), asi que corresponde bypass, igual que
        // SUPER_ADMIN (ver EmpresaFilter).
        rlsContextService.aplicarBypass();

        LocalDate fechaNotificacion = LocalDate.now().plusDays(diasNotificacion);
        LocalDateTime desde = fechaNotificacion.atStartOfDay();
        LocalDateTime hasta = fechaNotificacion.atTime(23, 59, 59);

        List<OrdenMantenimiento> ordenes =
        ordenRepository
            .findByEstadoAndTipoMantenimientoAndFechaProgramadaBetweenAndNotificacionProveedorEnviadaFalse(
                EstadoOrden.PROGRAMADA,
                TipoMantenimiento.PREVENTIVO,
                desde,
                hasta
            );

        for (OrdenMantenimiento orden : ordenes) {

            try {
                if (orden.getProveedor() == null || orden.getProveedor().getEmail() == null) {
                    log.warn("Orden {} sin proveedor/email asignado, se omite notificación", orden.getId());
                    continue;
                }

                log.info("Notificando orden {} ({}) a {}", orden.getId(), orden.getTitulo(),
                        orden.getProveedor().getEmail());

                emailService.sendEmailOrdenProgramada(orden.getProveedor().getEmail(), orden);

                // 📜 Se marca inmediatamente despues del envio exitoso,
                // dentro de la misma transaccion del metodo: si el
                // scheduler vuelve a correr el mismo dia (redeploy/
                // reinicio), la query ya no trae esta orden y no se
                // reenvia el email al proveedor.
                orden.setNotificacionProveedorEnviada(true);
                ordenRepository.save(orden);
            } catch (Exception e) {
                // 🔐 Una orden con error no debe abortar el envío de las
                // demás notificaciones del día (antes, una excepción aquí
                // revertía toda la transacción y ninguna orden del día
                // recibía notificación).
                log.error("Error notificando orden {}: {}", orden.getId(), e.getMessage(), e);
            }
        }

    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void eliminarOrdenesEmpresaDemo() {

        // 🔥 Este job solo debe correr en producción (donde vive la empresa
        // demo publica usada para mostrar el sistema a prospectos). En dev
        // se deshabilita via svua.scheduler.limpiezademo.enabled=false.
        if (!limpiezaDemoEnabled) {
            log.info("Limpieza de órdenes demo deshabilitada en este ambiente");
            return;
        }

        // 🔒 este job SI opera sobre una sola empresa conocida (la
        // demo), asi que se le informa esa empresa puntual a Postgres
        // en vez de hacer bypass completo (ver RlsContextService).
        rlsContextService.aplicarEmpresa(empresaId);

        log.info("Iniciando limpieza de órdenes de la empresa {}", empresaId);

        // Eliminar órdenes
        notificacionRepository.deleteByEmpresaId(empresaId);
        ordenRepuestoRepository.deleteByEmpresaId(empresaId);
        ordenReprogramacionRepository.deleteByEmpresaId(empresaId);
        historialEstadoActivoRepository.deleteByEmpresaIdAndComentarioNot(empresaId);
        ordenRepository.deleteByEmpresaId(empresaId);

        // Restaurar stock
        List<Repuesto> repuestos =
                repuestoRepository.findByEmpresaId(empresaId);

        repuestos.forEach(r -> r.setStockActual(STOCK_INICIAL_DEMO));

        repuestoRepository.saveAll(repuestos);

        // Restaurar estado activo
        List<Activo> activos =
                activoRepository.findByEmpresaId(empresaId);

        activos.forEach(r -> r.setEstadoActual(EstadoActivo.OPERATIVO));

        activoRepository.saveAll(activos);



        log.info("Limpieza finalizada correctamente");

    }

}
