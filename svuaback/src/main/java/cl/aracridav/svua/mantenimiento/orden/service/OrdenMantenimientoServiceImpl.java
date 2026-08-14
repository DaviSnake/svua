package cl.aracridav.svua.mantenimiento.orden.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.empresa.repository.EmpresaRepository;
import cl.aracridav.svua.inventario.activo.entity.Activo;
import cl.aracridav.svua.inventario.activo.repository.ActivoRepository;
import cl.aracridav.svua.inventario.historial.service.HistorialEstadoActivoService;
import cl.aracridav.svua.mantenimiento.orden.dto.request.ActualizarOrdenMantenimientoRequest;
import cl.aracridav.svua.mantenimiento.orden.dto.request.OrdenMantenimientoRequest;
import cl.aracridav.svua.mantenimiento.orden.dto.response.CostosGraficoReponse;
import cl.aracridav.svua.mantenimiento.orden.dto.response.OrdenEjecucionResponse;
import cl.aracridav.svua.mantenimiento.orden.dto.response.OrdenMantenimientoReporteResponse;
import cl.aracridav.svua.mantenimiento.orden.dto.response.OrdenMantenimientoResponse;
import cl.aracridav.svua.mantenimiento.orden.entity.EstadoOrden;
import cl.aracridav.svua.mantenimiento.orden.entity.OrdenMantenimiento;
import cl.aracridav.svua.mantenimiento.orden.entity.OrdenReprogramacion;
import cl.aracridav.svua.mantenimiento.orden.repository.OrdenMantenimientoRepository;
import cl.aracridav.svua.mantenimiento.orden.repository.OrdenReprogramacionRepository;
import cl.aracridav.svua.mantenimiento.ordenrepuesto.dto.request.OrdenRepuestoRequest;
import cl.aracridav.svua.mantenimiento.ordenrepuesto.dto.response.OrdenRepuestoResponse;
import cl.aracridav.svua.mantenimiento.ordenrepuesto.entity.OrdenRepuesto;
import cl.aracridav.svua.mantenimiento.ordenrepuesto.repository.OrdenRepuestoRepository;
import cl.aracridav.svua.mantenimiento.repuesto.entity.Repuesto;
import cl.aracridav.svua.mantenimiento.repuesto.repository.RepuestoRepository;
import cl.aracridav.svua.notificacion.service.NotificacionService;
import cl.aracridav.svua.proveedor.entity.Proveedor;
import cl.aracridav.svua.proveedor.repository.ProveedorRepository;
import cl.aracridav.svua.shared.enums.EstadoActivo;
import cl.aracridav.svua.shared.exception.BusinessException;
import cl.aracridav.svua.shared.mappers.GeneralMapper;
import cl.aracridav.svua.shared.util.SecurityUtils;
import cl.aracridav.svua.usuario.entity.Usuario;
import cl.aracridav.svua.usuario.repository.UsuarioRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class OrdenMantenimientoServiceImpl implements OrdenMantenimientoService {

    private final OrdenMantenimientoRepository ordenRepository;
    private final OrdenReprogramacionRepository ordenReprogramacionRepository;
    private final RepuestoRepository repuestoRepository;
    private final OrdenRepuestoRepository ordenRepuestoRepository;
    private final ActivoRepository activoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProveedorRepository proveedorRepository;
    private final EmpresaRepository empresaRepository;
    private final NotificacionService notificacionService;
    private final HistorialEstadoActivoService historialEstadoActivoService;
    private final GeneralMapper mapper;

    /*
     * =========================================
     * EJECUTAR ORDEN
     * =========================================
     */

    @Override
    public OrdenEjecucionResponse ejecutarOrden(Long idOrden) {

        return cambiarEstado(obtenerOrden(idOrden), EstadoOrden.EN_EJECUCION);

    }

    /*
     * =========================================
     * CERRAR ORDEN
     * =========================================
     */
    @Override
    public OrdenEjecucionResponse cerrarOrden(Long id, BigDecimal costo, String obs) {
        OrdenMantenimiento orden = obtenerOrden(id);
        orden.setCostoTotal(costo);
        orden.setObservaciones(obs);
        return cambiarEstado(orden, EstadoOrden.COMPLETADA);
    }

    /*
     * =========================================
     * DETENER ORDEN
     * =========================================
     */
    @Override
    public OrdenEjecucionResponse detenerOrden(Long idOrden) {
        return cambiarEstado(obtenerOrden(idOrden), EstadoOrden.COMPLETADA);
    }

    /*
     * =========================================
     * PRE DETENER ORDEN CON CHECK LIST
     * =========================================
     */
    @Override
    @Transactional
    public OrdenEjecucionResponse preDetenerOrden(Long id, MultipartFile archivo) {

        validarArchivo(archivo);

        OrdenMantenimiento orden = obtenerOrden(id);

        // 🔥 guardamos el archivo ANTES del cambio de estado
        String ruta = guardarArchivoSeguro(archivo, orden);

        orden.setRutaArchivo(ruta);

        // 🔥 reutilizamos CORE
        return cambiarEstado(orden, EstadoOrden.PRE_COMPLETADA);
    }


    /*
     * =========================================
     * CANCELAR ORDEN
     * =========================================
     */
    @Override
    @Transactional
    public void cancelarOrden(Long id, String motivo, Long usuarioId) {

        OrdenMantenimiento orden = ordenRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));

        // 🚫 solo se puede cancelar si está PROGRAMADA o PENDIENTE
        if (!(orden.getEstado() == EstadoOrden.PROGRAMADA
                || orden.getEstado() == EstadoOrden.PENDIENTE)) {
            throw new RuntimeException(
                    "Solo se pueden cancelar órdenes en estado PROGRAMADA o PENDIENTE"
            );
        }

        // 🔥 cambiar estado
        orden.setEstado(EstadoOrden.CANCELADA);

        // 🔥 opcional: registrar quién canceló
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        orden.setUsuarioFinalizacion(usuario);

        ordenRepository.save(orden);
    }

    /*
     * =========================================
     * CREACIÓN
     * =========================================
     */

    @Override
    public OrdenMantenimientoResponse crearOrden(OrdenMantenimientoRequest req) {

        Activo activo = obtenerActivo(req.getActivoId());
        Empresa empresa = obtenerEmpresaActual();
        Usuario usuario = obtenerUsuario(req.getUsuarioId());
        Proveedor proveedor = obtenerProveedor(req.getProveedorId());
        validarNoExisteOrdenPendiente(activo.getId());

        OrdenMantenimiento orden = construirOrden(
                req,
                empresa,
                activo,
                usuario,
                proveedor
        );

        // 🔥 guardar primero
        orden = ordenRepository.save(orden);

        // 🔥 guardar repuestos
        guardarRepuestosOrden(
                orden,
                req.getRepuestos(),
                usuario,
                empresa
        );

        return mapper.mapOrdenMantenimientoResponse(orden);
    }


    /*
     * =========================================
     * ACTUALIZACIÓN
     * =========================================
     */

    @Override
    @Transactional
    public OrdenMantenimientoResponse actualizar(Long id, ActualizarOrdenMantenimientoRequest request) {

        OrdenMantenimiento orden = ordenRepository.findById(id)
            .orElseThrow(() ->
                new RuntimeException("Orden no encontrada")
            );

        // =====================================================
        // VALIDAR ESTADO
        // =====================================================

        if (
            orden.getEstado() == EstadoOrden.EN_EJECUCION ||
            orden.getEstado() == EstadoOrden.CANCELADA ||
            orden.getEstado() == EstadoOrden.COMPLETADA
        ) {
            throw new RuntimeException(
                "No se puede editar la orden"
            );
        }

        // =====================================================
        // ACTUALIZAR DATOS PRINCIPALES
        // =====================================================
        orden.setTitulo(request.getTitulo());
        orden.setObservaciones(request.getObservaciones());

        orden.setTipoMantenimiento(
            request.getTipoMantenimiento()
        );

        // =====================================================
        // ACTUALIZAR REPUESTOS
        // =====================================================

        for (OrdenRepuesto old : orden.getRepuestosUtilizados()) {
            Repuesto repuesto = old.getRepuesto();

            repuesto.setStockActual(
                repuesto.getStockActual() + old.getCantidad()
            );

            Repuesto repuestoGuardado = repuestoRepository.save(repuesto);

            if (request.getRepuestos().isEmpty()) {
                notificacionService.verificarStockMinimo(repuestoGuardado);
            }

        }

        // 🔥 limpiar actuales
        orden.getRepuestosUtilizados().clear();

        if (
            request.getRepuestos() != null &&
            !request.getRepuestos().isEmpty()
        ) {

            List<OrdenRepuesto> nuevosRepuestos =
                new ArrayList<>();

            for (OrdenRepuestoRequest r : request.getRepuestos()) {

                Repuesto repuesto =
                    repuestoRepository.findById(
                        r.getRepuestoId()
                    ).orElseThrow(() ->
                        new RuntimeException(
                            "Repuesto no encontrado"
                        )
                    );

                int cantidad = r.getCantidad();

                // 🚨 VALIDAR STOCK
                if (repuesto.getStockActual() < cantidad) {
                    throw new BusinessException("Stock insuficiente para: " + repuesto.getNombre());
                }

                // 🔥 RESTAR STOCK
                repuesto.setStockActual(repuesto.getStockActual() - cantidad);

                Repuesto repuestoGuardado = repuestoRepository.save(repuesto);

                notificacionService.verificarStockMinimo(repuestoGuardado);

                OrdenRepuesto ordenRepuesto =
                    ordenRepuestoRepository
                        .findByOrdenIdAndRepuestoId(
                            orden.getId(),
                            repuesto.getId()
                        )
                        .orElse(null);

                if (ordenRepuesto == null) {
                    ordenRepuesto =
                        OrdenRepuesto.builder()
                            .orden(orden)
                            .repuesto(repuesto)
                            .cantidad(r.getCantidad())
                            .costoUnitario(repuesto.getCostoUnitario())
                            .costoTotal(
                                calcularCostoRepuesto(
                                    repuesto.getCostoUnitario(),
                                    r.getCantidad()
                                )
                            )
                            .empresa(orden.getEmpresa())
                            .usuario(orden.getUsuario())
                            .build();

                }
                else {
                    int nuevaCantidad =
                        ordenRepuesto.getCantidad() + r.getCantidad();

                    ordenRepuesto.setCantidad(nuevaCantidad);

                    ordenRepuesto.setCostoTotal(
                        calcularCostoRepuesto(repuesto.getCostoUnitario(), nuevaCantidad)
                    );
                }

                ordenRepuestoRepository.save(ordenRepuesto);
                nuevosRepuestos.add(ordenRepuesto);
            }

            orden.getRepuestosUtilizados()
                .addAll(nuevosRepuestos);
        }

        // =====================================================
        // CALCULAR COSTO TOTAL
        // =====================================================

        BigDecimal total = orden.getRepuestosUtilizados()
        .stream()
        .map(r -> Optional.ofNullable(r.getCostoTotal())
            .orElse(BigDecimal.ZERO))
        .reduce(BigDecimal.ZERO, BigDecimal::add);

        orden.setCostoTotal(total);

        // =====================================================
        // GUARDAR
        // =====================================================

        OrdenMantenimiento actualizada =
            ordenRepository.save(orden);

        return mapper.mapOrdenMantenimientoResponse(actualizada);
    }

    /*
     * =========================================
     * REPROGRAMAR
     * =========================================
     */

    @Override
    public OrdenMantenimientoResponse reprogramarOrden(Long id, LocalDateTime nuevaFecha, String motivo) {

        OrdenMantenimiento orden = obtenerOrden(id);

        validarEstadoReprogramacion(orden.getEstado());
        validarFechaReprogramacion(nuevaFecha);

        guardarHistorialReprogramacion(orden, nuevaFecha, motivo);

        orden.setFechaProgramada(nuevaFecha);
        orden.setFechaTermino(nuevaFecha.plusMinutes(orden.getDuracionSegundos()/60));

        return mapper.mapOrdenMantenimientoResponse(ordenRepository.save(orden));
    }

    /*
     * =========================================
     * OBTENER ARCHIIVO
     * =========================================
     */

    @Override
    public Resource obtenerArchivo(Long id) {

        try {

            OrdenMantenimiento orden = ordenRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Orden no encontrada"));

            Path path = Paths.get(orden.getRutaArchivo());

            if (!Files.exists(path)) {
                throw new BusinessException("Archivo no encontrado");
            }

            return new UrlResource(path.toUri());

        } catch (IOException e) {
            throw new BusinessException("Error al leer archivo");
        }
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

    @Override
    @Transactional(readOnly = true)
    public CostosGraficoReponse obtenerGraficoCostosUltimos6Meses(
            Long activoId, Long empresaIdFiltro) {

        // 🔒 Filtro por empresa opcional, solo para SUPER_ADMIN: para
        // el resto de roles se ignora y se usa siempre la empresa del
        // propio usuario (igual que antes).
        Long empresaId = (empresaIdFiltro != null && SecurityUtils.esSuperAdmin())
            ? empresaIdFiltro
            : SecurityUtils.getEmpresaId();

        LocalDateTime fechaInicio = LocalDateTime.now()
            .minusMonths(5)
            .withDayOfMonth(1)
            .withHour(0)
            .withMinute(0)
            .withSecond(0);

        // 🔥 Filtro por activo opcional: si no viene, se mantiene el
        // comportamiento actual (costos de toda la empresa).
        List<Object[]> resultados = activoId != null
            ? ordenRepository
                .obtenerCostosUltimosMesesPorActivo(fechaInicio, empresaId, activoId)
            : ordenRepository
                .obtenerCostosUltimosMeses(fechaInicio, empresaId);

        Map<Integer, BigDecimal> costosPorMes = new HashMap<>();

        for (Object[] r : resultados) {

            Integer mes = (Integer) r[1];

            BigDecimal total = r[2] != null
                ? (BigDecimal) r[2]
                : BigDecimal.ZERO;

            costosPorMes.put(mes, total);
        }

        List<String> categorias = new ArrayList<>();
        List<BigDecimal> series = new ArrayList<>();

        DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern(
                "MMM",
                Locale.forLanguageTag("es-CL")
            );

        for (int i = 5; i >= 0; i--) {

            LocalDate fecha = LocalDate.now().minusMonths(i);

            int mes = fecha.getMonthValue();

            categorias.add(
                    fecha.format(formatter).toUpperCase()
            );

            series.add(
                    costosPorMes.getOrDefault(
                            mes,
                            BigDecimal.ZERO
                    )
            );
        }

        return CostosGraficoReponse.builder()
                .categorias(categorias)
                .series(series)
                .build();
    }

    /*
     * =========================================
     * CORE
     * =========================================
     */

    private OrdenEjecucionResponse cambiarEstado(OrdenMantenimiento orden, EstadoOrden nuevoEstado) {

        Usuario usuario = getUsuarioActual();

        EstadoActivo viejoEstado = orden.getActivo().getEstadoActual();

        validarTransicion(orden.getEstado(), nuevoEstado);

        aplicarReglas(orden, nuevoEstado);

        orden.setEstado(nuevoEstado);

        if (nuevoEstado == EstadoOrden.PRE_COMPLETADA) {
            BigDecimal horasSistema = BigDecimal.valueOf(orden.getDuracionSegundos())
                .divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP);

            orden.setHorasRealesProveedor(horasSistema);

            orden.setCostoManoObraProveedor(
                calcularCosto(
                    horasSistema,
                    orden.getValorHoraProveedor()
                )
            );

            notificacionService.ordenPreCompletada(orden);
        }

        orden.setCostoTotal(
            calcularCostoTotal(orden)
        );

        OrdenMantenimiento ordenGuardada = ordenRepository.save(orden);

        actualizarEstadoActivo(ordenGuardada, nuevoEstado);

        EstadoActivo nuevoActivo = estadoActivoParaOrden(nuevoEstado);

        String comentario = ordenGuardada.getEstado() == EstadoOrden.COMPLETADA
            ? "Término mantención orden # " + orden.getId()
            : "Inicio mantención orden # " + orden.getId();

        if (nuevoEstado != EstadoOrden.PRE_COMPLETADA) {
            historialEstadoActivoService.registrarCambioEstado(
                ordenGuardada.getActivo().getId(), nuevoActivo, viejoEstado, comentario, usuario.getId()
            );
        }


        return mapper.mapOrdenEjecucionResponse(ordenGuardada);
    }

    private BigDecimal calcularCosto(BigDecimal horas, BigDecimal valorHora) {

        if (horas == null || valorHora == null) {
            return BigDecimal.ZERO;
        }

        return horas.multiply(valorHora);
    }

    private BigDecimal calcularCostoRepuesto(BigDecimal costoUnitario, int cantidad) {
        return costoUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    private BigDecimal calcularCostoTotal(OrdenMantenimiento orden) {

        BigDecimal costoManoObra = Optional.ofNullable(
            orden.getCostoManoObraProveedor()
        ).orElse(BigDecimal.ZERO);

        BigDecimal costoRepuestos = orden.getRepuestosUtilizados()
            .stream()
            .map(r ->
                Optional.ofNullable(r.getCostoUnitario())
                    .orElse(BigDecimal.ZERO)
                    .multiply(BigDecimal.valueOf(r.getCantidad()))
            )
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return costoManoObra.add(costoRepuestos);
    }

    private void actualizarEstadoActivo(OrdenMantenimiento orden, EstadoOrden estado) {

        // 🔥 el activo ya viene cargado en la orden (misma sesión/transacción),
        // se evita una consulta redundante a través de obtenerActivoDeOrden.
        Activo activo = orden.getActivo();

        activo.setEstadoActual(estadoActivoParaOrden(estado));

        activoRepository.save(activo);
    }

    private EstadoActivo estadoActivoParaOrden(EstadoOrden estado) {
        return (estado == EstadoOrden.COMPLETADA)
                ? EstadoActivo.OPERATIVO
                : EstadoActivo.FUERA_SERVICIO;
    }

    private void aplicarReglas(OrdenMantenimiento orden, EstadoOrden estado) {

        switch (estado) {

            case EN_EJECUCION -> iniciarOrden(orden);
            case PRE_COMPLETADA -> preFinalizarOrden(orden);
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

    private void preFinalizarOrden(OrdenMantenimiento orden) {

        if (orden.getFechaEjecucion() == null) {
            throw new BusinessException("Debe iniciar la orden primero");
        }

        calcularDuracion(orden);
        orden.setUsuarioPreFinalizacion(getUsuarioActual());
    }

    private void finalizarOrden(OrdenMantenimiento orden) {

        if (orden.getFechaEjecucion() == null) {
            throw new BusinessException("Debe iniciar la orden primero");
        }

        orden.setUsuarioFinalizacion(getUsuarioActual());
    }

    private void cancelarOrdenInterno(OrdenMantenimiento orden) {

        if (orden.getFechaEjecucion() != null) {
            calcularDuracion(orden);
        }

        orden.setUsuarioFinalizacion(getUsuarioActual());
    }

    private String guardarArchivoSeguro(MultipartFile archivo, OrdenMantenimiento orden) {

        try {
            String carpetaBase = "uploads/mantenimientos/";

            // 🔥 cada empresa guarda sus archivos en su propia subcarpeta
            // dentro de "mantenimientos": si no existe se crea, y si ya
            // existe simplemente se reutiliza (Files.createDirectories no
            // falla cuando la carpeta ya está creada).
            String carpetaEmpresa = obtenerCarpetaEmpresa(orden.getEmpresa());

            Path carpeta = Paths.get(carpetaBase, carpetaEmpresa);

            System.out.println("Ruta absoluta: " + carpeta.toAbsolutePath());

            if (!Files.exists(carpeta)) {
                Files.createDirectories(carpeta);
                System.out.println("Carpeta creada correctamente");
            }

            String nombreLimpio = StringUtils.cleanPath(archivo.getOriginalFilename());

            String nombreArchivo = orden.getId() + "_" + System.currentTimeMillis() + "_" + nombreLimpio;

            Path ruta = carpeta.resolve(nombreArchivo);

            Files.copy(archivo.getInputStream(), ruta, StandardCopyOption.REPLACE_EXISTING);

            return ruta.toString();

        } catch (IOException e) {
            throw new BusinessException("Error al guardar archivo");
        }
    }

    // 🔥 Nombre de carpeta seguro para el sistema de archivos: usa el id
    // de la empresa (estable y único) más su nombre saneado (sin tildes
    // ni caracteres especiales), para que la carpeta sea legible pero
    // nunca choque entre dos empresas con nombres parecidos.
    private String obtenerCarpetaEmpresa(Empresa empresa) {

        if (empresa == null || empresa.getId() == null) {
            return "sin_empresa";
        }

        String nombreSaneado = Normalizer.normalize(
                empresa.getNombre() != null ? empresa.getNombre() : "",
                Normalizer.Form.NFD
            )
            .replaceAll("\\p{M}", "")
            .replaceAll("[^a-zA-Z0-9]+", "_")
            .replaceAll("^_+|_+$", "");

        return nombreSaneado.isEmpty()
            ? String.valueOf(empresa.getId())
            : empresa.getId() + "_" + nombreSaneado;
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

    private Proveedor obtenerProveedor(Long id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Proveedor no existe"));
    }

    private Empresa obtenerEmpresaActual() {
        return empresaRepository.findById(SecurityUtils.getEmpresaId())
                .orElseThrow(() -> new BusinessException("Empresa no encontrada"));
    }

    private Usuario getUsuarioActual() {
        return obtenerUsuario(SecurityUtils.getUsuarioId());
    }

    private Repuesto obtenerRepuesto(Long id) {

        return repuestoRepository.findById(id)
            .orElseThrow(() ->
                new BusinessException(
                    "Repuesto no encontrado"
                )
            );
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
            Proveedor proveedor) {

        OrdenMantenimiento orden = new OrdenMantenimiento();

        orden.setTitulo(req.getTitulo());
        orden.setFechaProgramada(req.getFechaProgramada());

        // 🔥 AQUÍ agregas la lógica
        if (req.getFechaProgramada() != null) {
            LocalDateTime fechaTermino = req.getFechaProgramada().plusMinutes(req.getDuracionMinutos());
            orden.setFechaTermino(fechaTermino);
        }

        orden.setTipoMantenimiento(req.getTipoMantenimiento());
        orden.setEstado(EstadoOrden.PROGRAMADA);
        orden.setCostoTotal(req.getCostoTotal());
        orden.setDuracionSegundos(req.getDuracionMinutos()*60);
        orden.setHorasEstimadasProveedor(req.getHorasEstimadas());
        orden.setValorHoraProveedor(req.getValorHora());
        orden.setCostoManoObraEstimadasProveedor(req.getCostoManoObraEstimada());
        orden.setCostoManoObraProveedor(req.getCostoManoObra());
        orden.setObservaciones(req.getObservaciones());

        orden.setActivo(activo);
        orden.setUsuario(usuario);
        orden.setProveedor(proveedor);
        orden.setEmpresa(empresa);

        return orden;
    }

    private void guardarRepuestosOrden(
        OrdenMantenimiento orden,
        List<OrdenRepuestoRequest> repuestos,
        Usuario usuario,
        Empresa empresa) {

        if (repuestos == null || repuestos.isEmpty()) {
            return;
        }

        Set<OrdenRepuesto> lista = new HashSet<>();


        for (OrdenRepuestoRequest req : repuestos) {

            Repuesto repuesto =
                    obtenerRepuesto(req.getRepuestoId());

            // validar stock
            if (repuesto.getStockActual() < req.getCantidad()) {

                throw new BusinessException(
                    "Stock insuficiente para: "
                    + repuesto.getNombre()
                );
            }

            OrdenRepuesto item = new OrdenRepuesto();

            item.setOrden(orden);
            item.setRepuesto(repuesto);

            item.setCantidad(req.getCantidad());

            item.setCostoUnitario(
                    repuesto.getCostoUnitario()
            );

            item.setCostoTotal(
                    calcularCostoRepuesto(
                        repuesto.getCostoUnitario(),
                        req.getCantidad()
                    )
            );

            item.setUsuario(usuario);

            item.setEmpresa(empresa);

            lista.add(item);

            // 🔥 descontar stock
            repuesto.setStockActual(
                repuesto.getStockActual()
                    - req.getCantidad()
            );

            Repuesto repuestoGuardado =repuestoRepository.save(repuesto);

            notificacionService.verificarStockMinimo(repuestoGuardado);
        }

        ordenRepuestoRepository.saveAll(lista);

        orden.setRepuestosUtilizados(lista);
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

    // 🔥 Informe de Mantenciones: comprobante de ordenes COMPLETADAS,
    // filtrable por usuario/empresa/fecha, visible solo para SUPER_ADMIN.
    // Se arma con Specification (Criteria API) por la misma razon que el
    // informe de conexiones (SesionUsuarioServiceImpl): cada predicado solo
    // se agrega si el filtro viene informado, para no enviar nunca un
    // bind param sin tipo a Postgres.
    @Override
    @Transactional(readOnly = true)
    public Page<OrdenMantenimientoReporteResponse> obtenerInformeMantenciones(
            String usuario,
            Long empresaId,
            EstadoOrden estado,
            LocalDate fecha,
            Pageable pageable) {

        String usuarioFiltro =
            (usuario == null || usuario.isBlank())
                ? null
                : usuario.trim().toLowerCase();

        LocalDateTime desde = fecha != null ? fecha.atStartOfDay() : null;
        LocalDateTime hasta = fecha != null ? fecha.atTime(LocalTime.MAX) : null;

        Specification<OrdenMantenimiento> spec = (root, query, cb) -> {

            Join<OrdenMantenimiento, Empresa> empresaJoin =
                root.join("empresa", JoinType.INNER);
            Join<OrdenMantenimiento, Usuario> usuarioEjecucionJoin =
                root.join("usuarioEjecucion", JoinType.LEFT);
            Join<OrdenMantenimiento, Usuario> usuarioCreadorJoin =
                root.join("usuario", JoinType.INNER);

            // 🔥 El fetch solo aplica a la consulta de datos: en la
            // consulta de conteo (para la paginacion) Spring Data pide
            // resultType Long, y ahi un fetch no tiene sentido. No se hace
            // fetch de "repuestosUtilizados" (coleccion @OneToMany) porque
            // combinado con la paginacion produce el clasico problema de
            // Hibernate de paginar en memoria; se deja como lazy load
            // normal al mapear cada fila (la sesion sigue abierta porque
            // el metodo es @Transactional).
            if (query.getResultType() != Long.class) {
                root.fetch("activo", JoinType.INNER);
                root.fetch("empresa", JoinType.INNER);
                root.fetch("usuarioEjecucion", JoinType.LEFT);
                root.fetch("usuario", JoinType.INNER);
            }

            List<Predicate> predicates = new ArrayList<>();

            // 🔒 Filtro por estado opcional: el frontend parte con
            // "Completada" seleccionada (para que el informe siga
            // funcionando por defecto como comprobante de trabajos ya
            // ejecutados), pero el usuario puede elegir otro estado o
            // "Todos" para no filtrar por estado.
            if (estado != null) {
                predicates.add(cb.equal(root.get("estado"), estado));
            }

            if (usuarioFiltro != null) {
                predicates.add(
                    cb.or(
                        cb.like(cb.lower(usuarioEjecucionJoin.get("nombre")), "%" + usuarioFiltro + "%"),
                        cb.like(cb.lower(usuarioCreadorJoin.get("nombre")), "%" + usuarioFiltro + "%")
                    ));
            }

            if (empresaId != null) {
                predicates.add(cb.equal(empresaJoin.get("id"), empresaId));
            }

            if (desde != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fechaEjecucion"), desde));
            }

            if (hasta != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fechaEjecucion"), hasta));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 🔥 Orden fijo por fecha programada descendente, sin importar
        // el sort que traiga el Pageable del controller.
        Pageable pageableOrdenado = PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            Sort.by(Sort.Direction.DESC, "fechaProgramada"));

        return ordenRepository.findAll(spec, pageableOrdenado)
            .map(this::mapearInformeMantencion);
    }

    private OrdenMantenimientoReporteResponse mapearInformeMantencion(OrdenMantenimiento o) {

        List<OrdenRepuestoResponse> repuestos =
            o.getRepuestosUtilizados() == null
                ? List.of()
                : o.getRepuestosUtilizados().stream()
                    .map(this::mapearRepuestoInforme)
                    .toList();

        return OrdenMantenimientoReporteResponse.builder()
            .id(o.getId())
            .titulo(o.getTitulo())
            .observaciones(o.getObservaciones())
            .estado(o.getEstado())
            .tipoMantenimiento(o.getTipoMantenimiento())
            .fechaProgramada(o.getFechaProgramada())
            .fechaEjecucion(o.getFechaEjecucion())
            .duracionSegundos(o.getDuracionSegundos())
            .activoNombre(o.getActivo().getNombre())
            .empresaNombre(o.getEmpresa().getNombre())
            .usuarioNombre(
                o.getUsuarioEjecucion() != null
                    ? o.getUsuarioEjecucion().getNombre()
                    : o.getUsuario().getNombre())
            .proveedorNombre(
                o.getProveedor() != null
                    ? o.getProveedor().getNombre()
                    : null)
            .valorHoraProveedor(o.getValorHoraProveedor())
            .costoManoObraProveedor(o.getCostoManoObraProveedor())
            .costoTotal(o.getCostoTotal())
            .repuestos(repuestos)
            .build();
    }

    private OrdenRepuestoResponse mapearRepuestoInforme(OrdenRepuesto r) {

        OrdenRepuestoResponse dto = new OrdenRepuestoResponse();

        dto.setId(r.getId());
        dto.setRepuestoId(r.getRepuesto().getId());
        dto.setRepuestoNombre(r.getRepuesto().getNombre());
        dto.setCantidad(r.getCantidad());
        dto.setCostoUnitario(r.getCostoUnitario());
        dto.setCostoTotal(r.getCostoTotal());

        return dto;
    }
}
