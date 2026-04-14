package cl.aracridav.svua.inventario.historial.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.empresa.repository.EmpresaRepository;
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
    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ActivoRepository activoRepository;
    private final GeneralMapper generalMapper;

    @Override
    public void registrarCambioEstado(
            Long activoId,
            EstadoActivo nuevoEstado,
            String comentario) {

        Long usuarioId = SecurityUtils.getUsuarioId();

        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new BusinessException("Usuario no encontrado"));

        Empresa empresa = empresaRepository.findById(usuario.getEmpresa().getId())
            .orElseThrow(() -> new BusinessException("Empresa no encontrada"));

        Activo activo = activoRepository.findById(activoId)
                .orElseThrow(() -> 
                    new BusinessException("Activo no encontrado"));

        // 🔎 Verificar último estado
        Optional<HistorialEstadoActivo> ultimoOpt =
                historialRepository
                        .findTopByActivoIdOrderByFechaDesc(activoId);

        if (ultimoOpt.isPresent() &&
            ultimoOpt.get().getEstado() == nuevoEstado) {

            throw new BusinessException(
                    "El activo ya se encuentra en ese estado");
        }

        HistorialEstadoActivo historial = new HistorialEstadoActivo();
        historial.setActivo(activo);
        historial.setEstado(nuevoEstado);
        historial.setFecha(LocalDateTime.now());
        historial.setComentario(comentario);
        historial.setUsuario(usuario);
        historial.setEmpresa(empresa);

        historialRepository.save(historial);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistorialEstadoActivoResponse> 
        obtenerHistorial(Long activoId) {

        return historialRepository.findByActivoIdOrderByFechaAsc(activoId)
                .stream()
                .map(generalMapper::mapHistorialEstadoActivoResponse)
                .collect(Collectors.toList());
    }


    @Override
    @Transactional(readOnly = true)
    public HistorialEstadoActivoResponse obtenerUltimoEstado(Long activoId) {

    HistorialEstadoActivo historial = historialRepository
            .findTopByActivoIdOrderByFechaDesc(activoId)
            .orElseThrow(() ->
                    new BusinessException("No existe historial para el activo"));

    return generalMapper.mapHistorialEstadoActivoResponse(historial);
}

    @Override
    @Transactional(readOnly = true)
    public List<HistorialEstadoActivoResponse> 
        obtenerPorEstado(Long activoId, EstadoActivo estado) {

        return historialRepository.findByActivoIdAndEstado(activoId, estado)
                .stream()
                .map(generalMapper::mapHistorialEstadoActivoResponse)
                .collect(Collectors.toList());
    }
}