package cl.aracridav.svua.inventario.historial.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

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
import cl.aracridav.svua.shared.enums.EstadoActivo;
import cl.aracridav.svua.shared.exception.BusinessException;
import cl.aracridav.svua.shared.mappers.GeneralMapper;
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
                            .usuario(
                                historial.getUsuario() != null
                                    ? historial.getUsuario().getNombre()
                                    : null
                            )
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
                                orden.getFechaProgramada()
                            )
                            .tipo("MANTENCION")
                            .descripcion(
                                "Orden #"
                                    + orden.getId()
                                    + " - "
                                    + orden.getTitulo()
                                    + " ("
                                    + orden.getEstado()
                                    + ")"
                            )
                            .usuario(
                                orden.getUsuario() != null
                                    ? orden.getUsuario().getNombre()
                                    : null
                            )
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
    public List<HistorialActivoCompletoResponse> obtenerHistorialCompletoTodos() {

    List<Activo> activos = activoRepository.findAll();

    return activos.stream()
            .map(this::construirHistorialActivo)
            .toList();
}

    private HistorialActivoCompletoResponse construirHistorialActivo(
        Activo activo) {

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
                            .usuario(
                                historial.getUsuario() != null
                                    ? historial.getUsuario().getNombre()
                                    : null
                            )
                            .build()
                    );
                });
        }

        if (activo.getOrdenesMantenimiento() != null) {

            activo.getOrdenesMantenimiento()
                .forEach(orden -> {

                    eventos.add(
                        HistorialActivoResponse.builder()
                            .fecha(orden.getFechaProgramada())
                            .tipo("MANTENCION")
                            .descripcion(
                                "Orden #" + orden.getId()
                                + " - "
                                + orden.getTitulo()
                                + " (" + orden.getEstado() + ")"
                            )
                            .usuario(
                                orden.getUsuario() != null
                                    ? orden.getUsuario().getNombre()
                                    : null
                            )
                            .costoTotal(orden.getCostoTotal())
                            .valorHora(orden.getValorHoraProveedor())
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
                    throw new BusinessException("El activo ya se encuentra en ese estado");
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
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
    }

    private Activo obtenerActivo(Long id) {
        return activoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Activo no encontrado"));
    }
}