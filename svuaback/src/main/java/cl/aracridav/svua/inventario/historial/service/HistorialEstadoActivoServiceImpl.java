package cl.aracridav.svua.inventario.historial.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.aracridav.svua.inventario.activo.entity.Activo;
import cl.aracridav.svua.inventario.activo.repository.ActivoRepository;
import cl.aracridav.svua.inventario.historial.dto.response.HistorialEstadoActivoResponse;
import cl.aracridav.svua.inventario.historial.entity.HistorialEstadoActivo;
import cl.aracridav.svua.inventario.historial.repository.HistorialEstadoActivoRepository;
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
    public void registrarCambioEstado(Long activoId, EstadoActivo nuevoEstado, String comentario) {

        Usuario usuario = obtenerUsuarioActual();
        Activo activo = obtenerActivo(activoId);

        validarCambioEstado(activoId, nuevoEstado);

        HistorialEstadoActivo historial = construirHistorial(
                activo,
                nuevoEstado,
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
            String comentario,
            Usuario usuario) {

        HistorialEstadoActivo h = new HistorialEstadoActivo();

        h.setActivo(activo);
        h.setEstado(estado);
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

    private Usuario obtenerUsuarioActual() {
        return usuarioRepository.findById(SecurityUtils.getUsuarioId())
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
    }

    private Activo obtenerActivo(Long id) {
        return activoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Activo no encontrado"));
    }
}