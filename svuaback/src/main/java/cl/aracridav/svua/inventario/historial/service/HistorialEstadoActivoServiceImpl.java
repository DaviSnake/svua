package cl.aracridav.svua.inventario.historial.service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.aracridav.svua.inventario.activo.entity.Activo;
import cl.aracridav.svua.inventario.activo.repository.ActivoRepository;
import cl.aracridav.svua.inventario.historial.dto.response.HistorialActivoCompletoResponse;
import cl.aracridav.svua.inventario.historial.dto.response.HistorialActivoResponse;
import cl.aracridav.svua.inventario.historial.dto.response.HistorialEstadoActivoResponse;
import cl.aracridav.svua.inventario.historial.entity.HistorialEstadoActivo;
import cl.aracridav.svua.inventario.historial.repository.HistorialEstadoActivoRepository;
import cl.aracridav.svua.mantenimiento.orden.entity.OrdenMantenimiento;
import cl.aracridav.svua.proveedor.entity.Proveedor;
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
public class HistorialEstadoActivoServiceImpl implements HistorialEstadoActivoService {

    private final HistorialEstadoActivoRepository historialRepository;
    private final UsuarioRepository usuarioRepository;
    private final ActivoRepository activoRepository;
    private final GeneralMapper mapper;

    /*
     * =========================================
     * REGISTRAR CAMBIO
     * =========================================
     */
    @Override
    public void registrarCambioEstado(Long activoId, EstadoActivo nuevoEstado, EstadoActivo viejoEstado, String comentario, Long usuarioId) {

        Usuario usuario = obtenerUsuarioActual(usuarioId);
        Activo activo = obtenerActivo(activoId);

        validarCambioEstado(activoId, nuevoEstado);

        HistorialEstadoActivo historial = construirHistorial(
                activo,
                nuevoEstado,
                viejoEstado,
                comentario,
                usuario
        );

        historialRepository.save(historial);
    }

    /*
     * =========================================
     * CONSULTAS
     * =========================================
     */
    @Transactional(readOnly = true)
    @Override
    public HistorialActivoCompletoResponse obtenerHistorialCompleto(
        Long activoId) {

        Activo activo = activoRepository.findById(activoId)
                .orElseThrow(() ->
                        new BusinessException("Activo no encontrado"));

        List<HistorialActivoResponse> eventos =
                new ArrayList<>();

        // ==========================
        // CREACIÓN DEL ACTIVO
        // ==========================

        eventos.add(
            HistorialActivoResponse.builder()
                .fecha(activo.getFechaCreacion())
                .tipo("ACTIVO")
                .descripcion(
                    "Activo creado: "
                        + activo.getNombre()
                )
                .usuario(null)
                .costoTotal(null)
                .build()
        );

        // ==========================
        // CAMBIOS DE ESTADO
        // ==========================

        if (activo.getHistorialEstados() != null) {

            activo.getHistorialEstados()
                .forEach(historial -> {

                    String descripcion;

                    if (historial.getEstadoAnterior() == null) {

                        descripcion =
                            "Estado inicial: "
                                + historial.getEstado();

                    } else {

                        descripcion =
                            historial.getEstadoAnterior()
                                + " → "
                                + historial.getEstado();
                    }

                    eventos.add(
                        HistorialActivoResponse.builder()
                            .fecha(historial.getFecha())
                            .tipo("ESTADO")
                            .descripcion(descripcion)
                            .usuario(nombreDe(historial.getUsuario()))
                            .costoTotal(null)
                            .build()
                    );
                });
        }

        // ==========================
        // MANTENCIONES
        // ==========================

        if (activo.getOrdenesMantenimiento() != null) {

            activo.getOrdenesMantenimiento()
                .forEach(orden -> {

                    eventos.add(
                        HistorialActivoResponse.builder()
                            .fecha(
                                orden.getFechaEjecucion() != null
                                    ? orden.getFechaEjecucion()
                                    : orden.getFechaProgramada()
                            )
                            .fechaProgramada(
                                orden.getFechaProgramada()
                            )
                            .fechaEjecucion(
                                orden.getFechaEjecucion()
                            )
                            .tipo("MANTENCION")
                            .tipoMantenimiento(
                                orden.getTipoMantenimiento()
                            )
                            .descripcion(
                                "Orden #"
                                    + orden.getId()
                                    + " - "
                                    + orden.getTitulo()
                                    + " ("
                                    + orden.getEstado()
                                    + ")"
                            )
                            .usuario(nombreDe(orden.getUsuario()))
                            .costoTotal(
                                orden.getCostoTotal()
                            )
                            .valorHora(
                                orden.getValorHoraProveedor()
                            )
                            .costoManoObra(
                                orden.getCostoManoObraProveedor()
                            )
                            .horasTrabajo(
                                orden.getHorasRealesProveedor()
                            )
                            .build()
                    );
                });
        }

        // ==========================
        // ORDENAR TIMELINE
        // ==========================

        eventos.sort(
            Comparator.comparing(
                HistorialActivoResponse::getFecha
            ).reversed()
        );

        // ==========================
        // INDICADORES
        // ==========================

        BigDecimal costoMantenciones =
            activo.getOrdenesMantenimiento()
                .stream()
                .map(orden -> orden.getCostoTotal())
                .filter(Objects::nonNull)
                .reduce(
                    BigDecimal.ZERO,
                    BigDecimal::add
                );

        Integer cantidadMantenciones =
            activo.getOrdenesMantenimiento()
                .size();

        // ==========================
        // RESPONSE
        // ==========================

        return HistorialActivoCompletoResponse.builder()
            .activoId(
                activo.getId()
            )
            .nombreActivo(
                activo.getNombre()
            )
            .valorAdquisicion(
                activo.getValorAdquisicion()
            )
            .valorResidual(
                activo.getValorResidual()
            )
            .costoMantenciones(
                costoMantenciones
            )
            .cantidadMantenciones(
                cantidadMantenciones
            )
            .empresaId(
                activo.getEmpresa() != null ? activo.getEmpresa().getId() : null
            )
            .empresaNombre(
                activo.getEmpresa() != null ? activo.getEmpresa().getNombre() : null
            )
            .eventos(
                eventos
            )
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistorialEstadoActivoResponse> obtenerHistorial(Long activoId) {

        return historialRepository.findByActivoIdOrderByFechaAsc(activoId)
                .stream()
                .map(mapper::mapHistorialEstadoActivoResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public HistorialEstadoActivoResponse obtenerUltimoEstado(Long activoId) {

        HistorialEstadoActivo historial = historialRepository
                .findTopByActivoIdOrderByFechaDesc(activoId)
                .orElseThrow(() ->
                        new BusinessException("No existe historial para el activo"));

        return mapper.mapHistorialEstadoActivoResponse(historial);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistorialEstadoActivoResponse> obtenerPorEstado(Long activoId, EstadoActivo estado) {

        return historialRepository.findByActivoIdAndEstado(activoId, estado)
                .stream()
                .map(mapper::mapHistorialEstadoActivoResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistorialActivoCompletoResponse> obtenerHistorialCompletoTodos(Long empresaId) {

        List<Activo> activos;

        if (esSuperAdmin()) {
            // 🔥 SUPER_ADMIN puede ver todas las empresas o filtrar por una
            activos = (empresaId != null)
                    ? activoRepository.findAllConHistorialByEmpresa(empresaId)
                    : activoRepository.findAllConHistorial();
        } else {
            // 🔒 Usuarios no SUPER_ADMIN siempre ven solo su propia empresa,
            // sin importar lo que llegue en empresaId (mismo criterio que
            // el resto de los mantenedores). Antes de este fix, aquí no
            // existía ninguna restricción: cualquier usuario veía el
            // historial de TODAS las empresas.
            activos = activoRepository.findAllConHistorialByEmpresa(
                    SecurityUtils.getEmpresaId());
        }

        return activos.stream()
                .map(this::construirHistorialActivo)
                .toList();
    }

    private HistorialActivoCompletoResponse construirHistorialActivo(
        Activo activo) {

        NumberFormat clp =
            NumberFormat.getCurrencyInstance(
                Locale.of("es", "CL")
            );

        List<HistorialActivoResponse> eventos =
                new ArrayList<>();

        eventos.add(
            HistorialActivoResponse.builder()
                .fecha(activo.getFechaCreacion())
                .tipo("ACTIVO")
                .descripcion("Activo creado: " + activo.getNombre())
                .build()
        );

        if (activo.getHistorialEstados() != null) {

            activo.getHistorialEstados()
                .forEach(historial -> {

                    String descripcion =
                        historial.getEstadoAnterior() == null
                            ? "Estado inicial: " + historial.getEstado()
                            : historial.getEstadoAnterior()
                                + " → "
                                + historial.getEstado();

                    eventos.add(
                        HistorialActivoResponse.builder()
                            .fecha(historial.getFecha())
                            .tipo("ESTADO")
                            .descripcion(descripcion)
                            .usuario(nombreDe(historial.getUsuario()))
                            .build()
                    );
                });
        }

        if (activo.getOrdenesMantenimiento() != null) {

            activo.getOrdenesMantenimiento()
                .forEach(orden -> {

                    List<String> repuestos = new ArrayList<>();

                    if (orden.getRepuestosUtilizados() != null) {

                        repuestos = orden.getRepuestosUtilizados()
                            .stream()
                            .map(r ->
                                r.getRepuesto().getNombre()
                                + " x"
                                + r.getCantidad()
                                + " ("
                                + clp.format(r.getCostoTotal())
                                + ")"
                            )
                            .distinct()
                            .toList();
                    }

                    eventos.add(
                        HistorialActivoResponse.builder()
                            .fecha(
                                orden.getFechaEjecucion() != null
                                    ? orden.getFechaEjecucion()
                                    : orden.getFechaProgramada()
                            )
                            .fechaProgramada(orden.getFechaProgramada())
                            .fechaEjecucion(orden.getFechaEjecucion())
                            .tipo("MANTENCION")
                            .tipoMantenimiento(orden.getTipoMantenimiento())
                            .descripcion(
                                "Orden #" + orden.getId()
                                + " - "
                                + orden.getTitulo()
                                + " (" + orden.getEstado() + ")"
                            )
                            .usuario(nombreDe(orden.getUsuario()))
                            .proveedor(nombreDe(orden.getProveedor()))
                            .costoTotal(orden.getCostoTotal())
                            .valorHora(orden.getValorHoraProveedor())
                            .costoManoObra(
                                orden.getCostoManoObraProveedor()
                            )
                            .horasTrabajo(
                                orden.getHorasRealesProveedor()
                            )
                            .repuestos(repuestos)
                            .build()
                    );
                });
        }

        eventos.sort(
            Comparator.comparing(
                HistorialActivoResponse::getFecha
            ).reversed()
        );

        BigDecimal costoMantenciones =
            activo.getOrdenesMantenimiento()
                .stream()
                .map(OrdenMantenimiento::getCostoTotal)
                .filter(Objects::nonNull)
                .reduce(
                    BigDecimal.ZERO,
                    BigDecimal::add
                );

        return HistorialActivoCompletoResponse.builder()
            .activoId(activo.getId())
            .nombreActivo(activo.getNombre())
            .valorAdquisicion(activo.getValorAdquisicion())
            .valorResidual(activo.getValorResidual())
            .costoMantenciones(costoMantenciones)
            .cantidadMantenciones(
                activo.getOrdenesMantenimiento().size()
            )
            .empresaId(
                activo.getEmpresa() != null ? activo.getEmpresa().getId() : null
            )
            .empresaNombre(
                activo.getEmpresa() != null ? activo.getEmpresa().getNombre() : null
            )
            .eventos(eventos)
            .build();
    }

    /*
     * =========================================
     * VALIDACIONES
     * =========================================
     */

    private void validarCambioEstado(Long activoId, EstadoActivo nuevoEstado) {

        historialRepository
                .findTopByActivoIdOrderByFechaDesc(activoId)
                .filter(h -> h.getEstado() == nuevoEstado)
                .ifPresent(h -> {
                    throw new BusinessException("El activo ya se encuentra en ese estado" + " (" + nuevoEstado + ")");
                });
    }

    /*
     * =========================================
     * BUILDER
     * =========================================
     */

    private HistorialEstadoActivo construirHistorial(
            Activo activo,
            EstadoActivo estado,
            EstadoActivo viejoEstado,
            String comentario,
            Usuario usuario) {

        HistorialEstadoActivo h = new HistorialEstadoActivo();

        h.setActivo(activo);
        h.setEstado(estado);
        h.setEstadoAnterior(viejoEstado);
        h.setFecha(LocalDateTime.now());
        h.setComentario(comentario);
        h.setUsuario(usuario);
        h.setEmpresa(usuario.getEmpresa()); // 🔥 más limpio

        return h;
    }

    /*
     * =========================================
     * HELPERS
     * =========================================
     */

    private Usuario obtenerUsuarioActual(Long usuarioId) {
        // 🔧 La mayoria de los llamados a registrarCambioEstado no traen un
        // usuarioId explicito (pasan null): en ese caso se usa el usuario
        // autenticado actual. Antes de este fix, findById(null) reventaba
        // con IllegalArgumentException al crear/editar/dar de baja un activo.
        Long id = usuarioId != null ? usuarioId : SecurityUtils.getUsuarioId();
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
    }

    private Activo obtenerActivo(Long id) {
        return activoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Activo no encontrado"));
    }

    private String nombreDe(Usuario usuario) {
        return usuario != null ? usuario.getNombre() : null;
    }

    private String nombreDe(Proveedor proveedor) {
        return proveedor != null ? proveedor.getNombre() : null;
    }

    private boolean esSuperAdmin() {
        return SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
    }
}
