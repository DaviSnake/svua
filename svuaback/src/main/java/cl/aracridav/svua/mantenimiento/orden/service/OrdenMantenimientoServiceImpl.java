package cl.aracridav.svua.mantenimiento.orden.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.empresa.repository.EmpresaRepository;
import cl.aracridav.svua.inventario.activo.entity.Activo;
import cl.aracridav.svua.inventario.activo.repository.ActivoRepository;
import cl.aracridav.svua.mantenimiento.orden.dto.request.OrdenMantenimientoRequest;
import cl.aracridav.svua.mantenimiento.orden.dto.response.OrdenEjecucionResponse;
import cl.aracridav.svua.mantenimiento.orden.dto.response.OrdenMantenimientoResponse;
import cl.aracridav.svua.mantenimiento.orden.entity.EstadoOrden;
import cl.aracridav.svua.mantenimiento.orden.entity.OrdenMantenimiento;
import cl.aracridav.svua.mantenimiento.orden.entity.OrdenReprogramacion;
import cl.aracridav.svua.mantenimiento.orden.repository.OrdenMantenimientoRepository;
import cl.aracridav.svua.mantenimiento.orden.repository.OrdenReprogramacionRepository;
import cl.aracridav.svua.mantenimiento.plan.entity.PlanMantenimiento;
import cl.aracridav.svua.mantenimiento.plan.repository.PlanMantenimientoRepository;
import cl.aracridav.svua.shared.enums.EstadoActivo;
import cl.aracridav.svua.shared.exception.BusinessException;
import cl.aracridav.svua.shared.mappers.GeneralMapper;
import cl.aracridav.svua.shared.util.SecurityUtils;
import cl.aracridav.svua.usuario.entity.Usuario;
import cl.aracridav.svua.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class OrdenMantenimientoServiceImpl implements OrdenMantenimientoService {

    private final OrdenMantenimientoRepository ordenRepository;
    private final OrdenReprogramacionRepository ordenReprogramacionRepository;
    private final ActivoRepository activoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PlanMantenimientoRepository planRepository;
    private final EmpresaRepository empresaRepository;
    private final GeneralMapper mapper;

    /*
     * =========================================
     * ESTADOS
     * =========================================
     */

    @Override
    public OrdenEjecucionResponse ejecutarOrden(Long idOrden) {
        return cambiarEstado(obtenerOrden(idOrden), EstadoOrden.EN_EJECUCION);
    }

    @Override
    public OrdenEjecucionResponse cerrarOrden(Long id, BigDecimal costo, String obs) {
        OrdenMantenimiento orden = obtenerOrden(id);
        orden.setCosto(costo);
        orden.setObservaciones(obs);
        return cambiarEstado(orden, EstadoOrden.COMPLETADA);
    }

    @Override
    public OrdenEjecucionResponse detenerOrden(Long idOrden) {
        return cambiarEstado(obtenerOrden(idOrden), EstadoOrden.COMPLETADA);
    }

    @Override
    @Transactional
    public OrdenEjecucionResponse detenerOrden(Long id, MultipartFile archivo) {

        validarArchivo(archivo);

        OrdenMantenimiento orden = obtenerOrden(id);

        // 🔥 guardamos el archivo ANTES del cambio de estado
        String ruta = guardarArchivoSeguro(archivo, orden.getId());

        orden.setRutaArchivo(ruta);

        // 🔥 reutilizamos CORE
        return cambiarEstado(orden, EstadoOrden.COMPLETADA);
    }

    @Override
    public OrdenMantenimiento cancelarOrden(Long id, String motivo) {
        OrdenMantenimiento orden = obtenerOrden(id);

        if (orden.getEstado() == EstadoOrden.COMPLETADA) {
            throw new BusinessException("No se puede cancelar una orden cerrada");
        }

        orden.setEstado(EstadoOrden.CANCELADA);
        orden.setObservaciones(motivo);

        return ordenRepository.save(orden);
    }

    /*
     * =========================================
     * CREACIÓN
     * =========================================
     */

    @Override
    public OrdenMantenimientoResponse crearOrden(OrdenMantenimientoRequest req) {

        Activo activo = obtenerActivo(req.getActivoId());
        validarNoExisteOrdenPendiente(activo.getId());

        OrdenMantenimiento orden = construirOrden(
                req,
                obtenerEmpresaActual(),
                activo,
                obtenerUsuario(req.getUsuarioId()),
                obtenerPlan(req.getPlanMantenimientoId())
        );

        return mapper.mapOrdenMantenimientoResponse(ordenRepository.save(orden));
    }

    @Override
    public OrdenMantenimiento generarDesdePlan(Long planId, Long usuarioId) {

        PlanMantenimiento plan = obtenerPlan(planId);
        validarPlanActivo(plan);

        validarNoExisteOrdenPendiente(plan.getActivo().getId());

        OrdenMantenimiento orden = new OrdenMantenimiento();
        orden.setActivo(plan.getActivo());
        orden.setUsuario(obtenerUsuario(usuarioId));
        orden.setTipoMantenimiento(plan.getTipoMantenimiento());
        orden.setFechaProgramada(plan.getProximaEjecucion());
        orden.setEstado(EstadoOrden.PENDIENTE);
        orden.setPlanMantenimiento(plan);

        return ordenRepository.save(orden);
    }

    /*
     * =========================================
     * ACTUALIZACIÓN
     * =========================================
     */

    @Override
    public OrdenMantenimientoResponse actualizarOrden(Long id, OrdenMantenimientoRequest req) {

        OrdenMantenimiento orden = obtenerOrden(id);

        orden.setTitulo(req.getTitulo());
        orden.setFechaProgramada(req.getFechaProgramada());

        return mapper.mapOrdenMantenimientoResponse(ordenRepository.save(orden));
    }

    @Override
    public OrdenMantenimientoResponse reprogramarOrden(Long id, LocalDateTime nuevaFecha, String motivo) {

        OrdenMantenimiento orden = obtenerOrden(id);

        validarEstadoReprogramacion(orden.getEstado());
        validarFechaReprogramacion(nuevaFecha);

        guardarHistorialReprogramacion(orden, nuevaFecha, motivo);

        orden.setFechaProgramada(nuevaFecha);

        return mapper.mapOrdenMantenimientoResponse(ordenRepository.save(orden));
    }

    /*
     * =========================================
     * CONSULTAS
     * =========================================
     */

    @Override
    @Transactional(readOnly = true)
    public List<OrdenMantenimiento> obtenerOrdenesVencidas() {
        return ordenRepository.findOrdenesVencidas(LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdenMantenimientoResponse> listarOrdenesEmpresa() {

        Long empresaId = SecurityUtils.getEmpresaId();

        return ordenRepository.findByEmpresaId(empresaId)
                .stream()
                .map(mapper::mapOrdenMantenimientoResponse)
                .toList();
    }

    /*
     * =========================================
     * CORE
     * =========================================
     */

    private OrdenEjecucionResponse cambiarEstado(OrdenMantenimiento orden, EstadoOrden nuevoEstado) {

        validarTransicion(orden.getEstado(), nuevoEstado);

        aplicarReglas(orden, nuevoEstado);

        orden.setEstado(nuevoEstado);

        OrdenMantenimiento guardada = ordenRepository.save(orden);

        actualizarEstadoActivo(guardada, nuevoEstado);

        return mapper.mapOrdenEjecucionResponse(guardada);
    }

    private void actualizarEstadoActivo(OrdenMantenimiento orden, EstadoOrden estado) {

        Activo activo = obtenerActivoDeOrden(orden.getId());

        EstadoActivo nuevoEstado = (estado == EstadoOrden.COMPLETADA)
                ? EstadoActivo.OPERATIVO
                : EstadoActivo.FUERA_SERVICIO;

        activo.setEstadoActual(nuevoEstado);

        activoRepository.save(activo);
    }

    private void aplicarReglas(OrdenMantenimiento orden, EstadoOrden estado) {

        switch (estado) {

            case EN_EJECUCION -> iniciarOrden(orden);
            case COMPLETADA -> finalizarOrden(orden);
            case CANCELADA -> cancelarOrdenInterno(orden);
            default -> throw new IllegalArgumentException("Unexpected value: " + estado);

        }
    }

    private void iniciarOrden(OrdenMantenimiento orden) {

        if (orden.getActivo().getEstadoActual() == EstadoActivo.BAJA) {
            throw new BusinessException("Activo dado de baja");
        }

        orden.setFechaEjecucion(LocalDateTime.now());
        orden.setUsuarioEjecucion(getUsuarioActual());
    }

    private void finalizarOrden(OrdenMantenimiento orden) {

        if (orden.getFechaEjecucion() == null) {
            throw new BusinessException("Debe iniciar la orden primero");
        }

        calcularDuracion(orden);
        orden.setUsuarioFinalizacion(getUsuarioActual());
    }

    private void cancelarOrdenInterno(OrdenMantenimiento orden) {

        if (orden.getFechaEjecucion() != null) {
            calcularDuracion(orden);
        }

        orden.setUsuarioFinalizacion(getUsuarioActual());
    }

    private String guardarArchivoSeguro(MultipartFile archivo, Long ordenId) {

        try {
            String carpetaBase = "uploads/mantenimientos/";

            Path carpeta = Paths.get(carpetaBase);

            if (!Files.exists(carpeta)) {
                Files.createDirectories(carpeta);
            }

            String nombreLimpio = StringUtils.cleanPath(archivo.getOriginalFilename());

            String nombreArchivo = ordenId + "_" + System.currentTimeMillis() + "_" + nombreLimpio;

            Path ruta = carpeta.resolve(nombreArchivo);

            Files.copy(archivo.getInputStream(), ruta, StandardCopyOption.REPLACE_EXISTING);

            return ruta.toString();

        } catch (IOException e) {
            throw new BusinessException("Error al guardar archivo");
        }
    }

    private void calcularDuracion(OrdenMantenimiento orden) {

        LocalDateTime ahora = LocalDateTime.now();

        orden.setFechaFinEjecucion(ahora);

        long duracion = Duration
                .between(orden.getFechaEjecucion(), ahora)
                .getSeconds();

        orden.setDuracionSegundos(duracion);
    }

    private void validarArchivo(MultipartFile archivo) {

    if (archivo == null || archivo.isEmpty()) {
        throw new BusinessException("Debe adjuntar un archivo");
    }

    if (archivo.getSize() > 5 * 1024 * 1024) {
        throw new BusinessException("Archivo supera 5MB");
    }
}

    /*
     * =========================================
     * HELPERS
     * =========================================
     */

    private OrdenMantenimiento obtenerOrden(Long id) {
        return ordenRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Orden no existe"));
    }

    private Activo obtenerActivo(Long id) {
        return activoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Activo no existe"));
    }

    private Usuario obtenerUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Usuario no existe"));
    }

    private PlanMantenimiento obtenerPlan(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Plan no existe"));
    }

    private Empresa obtenerEmpresaActual() {
        return empresaRepository.findById(SecurityUtils.getEmpresaId())
                .orElseThrow(() -> new BusinessException("Empresa no encontrada"));
    }

    private Usuario getUsuarioActual() {
        return obtenerUsuario(SecurityUtils.getUsuarioId());
    }

    private Activo obtenerActivoDeOrden(Long ordenId) {
        return ordenRepository.findActivoByOrdenId(ordenId)
                .orElseThrow(() -> new RuntimeException("Activo no encontrado"));
    }

    /*
     * =========================================
     * VALIDACIONES
     * =========================================
     */

    private void validarTransicion(EstadoOrden actual, EstadoOrden nuevo) {
        if (!actual.puedePasarA(nuevo)) {
            throw new BusinessException("Transición inválida: " + actual + " → " + nuevo);
        }
    }

    private void validarPlanActivo(PlanMantenimiento plan) {
        if (!plan.getEstaActivo()) {
            throw new BusinessException("El plan no está activo");
        }
    }

    private void validarNoExisteOrdenPendiente(Long activoId) {
        if (ordenRepository.existsByActivoIdAndEstado(activoId, EstadoOrden.PENDIENTE)) {
            throw new BusinessException("Ya existe una orden pendiente para este activo");
        }
    }

    private void validarEstadoReprogramacion(EstadoOrden estado) {
        if (estado != EstadoOrden.PENDIENTE && estado != EstadoOrden.PROGRAMADA) {
            throw new BusinessException("Solo se puede reprogramar en estado PENDIENTE o PROGRAMADA");
        }
    }

    private void validarFechaReprogramacion(LocalDateTime fecha) {
        if (fecha == null || fecha.isBefore(LocalDateTime.now())) {
            throw new BusinessException("Fecha inválida");
        }
    }

    /*
     * =========================================
     * BUILDER
     * =========================================
     */

    private OrdenMantenimiento construirOrden(
            OrdenMantenimientoRequest req,
            Empresa empresa,
            Activo activo,
            Usuario usuario,
            PlanMantenimiento plan) {

        OrdenMantenimiento orden = new OrdenMantenimiento();

        orden.setTitulo(req.getTitulo());
        orden.setFechaProgramada(req.getFechaProgramada());
        orden.setTipoMantenimiento(req.getTipoMantenimiento());
        orden.setEstado(EstadoOrden.PROGRAMADA);
        orden.setCosto(req.getCosto());
        orden.setObservaciones(req.getObservaciones());

        orden.setActivo(activo);
        orden.setUsuario(usuario);
        orden.setPlanMantenimiento(plan);
        orden.setEmpresa(empresa);

        return orden;
    }

    private void guardarHistorialReprogramacion(OrdenMantenimiento orden, LocalDateTime nuevaFecha, String motivo) {

        OrdenReprogramacion r = new OrdenReprogramacion();
        r.setOrden(orden);
        r.setFechaAnterior(orden.getFechaProgramada());
        r.setFechaNueva(nuevaFecha);
        r.setUsuario(getUsuarioActual());
        r.setEmpresa(obtenerEmpresaActual());
        r.setMotivo(motivo);

        ordenReprogramacionRepository.save(r);
    }
}
