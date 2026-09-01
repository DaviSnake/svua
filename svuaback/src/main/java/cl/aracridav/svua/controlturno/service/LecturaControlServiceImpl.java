package cl.aracridav.svua.controlturno.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.aracridav.svua.controlturno.dto.request.LecturaControlRequest;
import cl.aracridav.svua.controlturno.dto.response.LecturaControlResponse;
import cl.aracridav.svua.controlturno.dto.response.PuntoControlDashboardResponse;
import cl.aracridav.svua.controlturno.entity.LecturaControl;
import cl.aracridav.svua.controlturno.entity.PuntoControl;
import cl.aracridav.svua.controlturno.enums.TurnoTrabajo;
import cl.aracridav.svua.controlturno.repository.LecturaControlRepository;
import cl.aracridav.svua.controlturno.repository.LecturaControlSpecs;
import cl.aracridav.svua.controlturno.repository.PuntoControlRepository;
import cl.aracridav.svua.shared.exception.BusinessException;
import cl.aracridav.svua.shared.util.SecurityUtils;
import cl.aracridav.svua.usuario.entity.Usuario;
import cl.aracridav.svua.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

// 🔥 Registro de lecturas horarias por turno y su dashboard de graficos
// pre-agregados (ver PuntoControlDashboardResponse) -- reemplaza a la
// planilla Excel "SISTEMA_DE_CONTROL_DE_MANTENCION".
@Service
@RequiredArgsConstructor
@Transactional
public class LecturaControlServiceImpl implements LecturaControlService {

    private final LecturaControlRepository repository;
    private final PuntoControlRepository puntoControlRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    public LecturaControlResponse registrar(LecturaControlRequest request) {

        validarControlTurnoHabilitado();
        validarRequest(request);

        Long empresaId = SecurityUtils.getEmpresaId();

        PuntoControl puntoControl = puntoControlRepository.findById(request.getPuntoControlId())
                .orElseThrow(() -> new BusinessException("Punto de control no encontrado"));

        if (!puntoControl.getEmpresa().getId().equals(empresaId)) {
            throw new BusinessException("El punto de control no pertenece a su empresa");
        }

        if (!Boolean.TRUE.equals(puntoControl.getActivo())) {
            throw new BusinessException("El punto de control está deshabilitado");
        }

        Usuario usuario = usuarioRepository.findById(SecurityUtils.getUsuarioId())
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));

        LecturaControl lectura = new LecturaControl();
        lectura.setPuntoControl(puntoControl);
        lectura.setValor(request.getValor());
        lectura.setFechaHora(request.getFechaHora() != null ? request.getFechaHora() : LocalDateTime.now());
        lectura.setTurno(request.getTurno());
        lectura.setObservacion(request.getObservacion());
        lectura.setUsuario(usuario);
        lectura.setEmpresa(puntoControl.getEmpresa());

        return mapResponse(repository.save(lectura));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LecturaControlResponse> listar(
            Pageable pageable, Long puntoControlId,
            LocalDateTime desde, LocalDateTime hasta, TurnoTrabajo turno) {

        validarControlTurnoHabilitado();

        Long empresaId = SecurityUtils.getEmpresaId();

        Specification<LecturaControl> spec = Specification
                .where(LecturaControlSpecs.empresaId(empresaId))
                .and(LecturaControlSpecs.puntoControlId(puntoControlId))
                .and(LecturaControlSpecs.desde(desde))
                .and(LecturaControlSpecs.hasta(hasta))
                .and(LecturaControlSpecs.turno(turno));

        return repository.findAll(spec, pageable).map(this::mapResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PuntoControlDashboardResponse> dashboard(
            Long puntoControlId, LocalDateTime desde, LocalDateTime hasta, TurnoTrabajo turno) {

        validarControlTurnoHabilitado();

        Long empresaId = SecurityUtils.getEmpresaId();

        // 🔥 Sin rango explicito, se muestra solo el dia actual (00:00 de
        // hoy hasta este instante) -- el dashboard es para seguimiento del
        // turno en curso, no un historico. Para ver dias anteriores hay
        // que usar el filtro de fecha explicito (mismo endpoint).
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime desdeEfectivo = desde != null ? desde : ahora.toLocalDate().atStartOfDay();
        LocalDateTime hastaEfectivo = hasta != null ? hasta : ahora;

        List<PuntoControl> puntos;

        if (puntoControlId != null) {

            PuntoControl punto = puntoControlRepository.findById(puntoControlId)
                    .filter(p -> p.getEmpresa().getId().equals(empresaId))
                    .orElseThrow(() -> new BusinessException("Punto de control no encontrado"));

            // 🔒 Un punto deshabilitado no debe graficarse aunque se pida
            // explícitamente por id (defensa en profundidad: el combo del
            // frontend ya solo ofrece puntos activos para elegir).
            puntos = Boolean.TRUE.equals(punto.getActivo()) ? List.of(punto) : List.of();

        } else {
            puntos = puntoControlRepository.findByEmpresaIdAndActivoTrue(empresaId);
        }

        List<PuntoControlDashboardResponse> resultado = new ArrayList<>();

        for (PuntoControl punto : puntos) {

            Specification<LecturaControl> specGrafico = Specification
                    .where(LecturaControlSpecs.empresaId(empresaId))
                    .and(LecturaControlSpecs.puntoControlId(punto.getId()))
                    .and(LecturaControlSpecs.desde(desdeEfectivo))
                    .and(LecturaControlSpecs.hasta(hastaEfectivo))
                    .and(LecturaControlSpecs.turno(turno));

            List<LecturaControl> lecturas = repository.findAll(specGrafico, Sort.by(Sort.Direction.ASC, "fechaHora"));

            List<LocalDateTime> fechas = new ArrayList<>();
            List<BigDecimal> valores = new ArrayList<>();
            long dentroRango = 0;
            long fueraRango = 0;

            for (LecturaControl lectura : lecturas) {

                fechas.add(lectura.getFechaHora());
                valores.add(lectura.getValor());

                if (punto.getValorMin() != null && punto.getValorMax() != null) {
                    boolean dentro = lectura.getValor().compareTo(punto.getValorMin()) >= 0
                            && lectura.getValor().compareTo(punto.getValorMax()) <= 0;
                    if (dentro) {
                        dentroRango++;
                    } else {
                        fueraRango++;
                    }
                }
            }

            resultado.add(PuntoControlDashboardResponse.builder()
                    .puntoControlId(punto.getId())
                    .nombre(punto.getNombre())
                    .unidad(punto.getUnidad())
                    .valorMin(punto.getValorMin())
                    .valorMax(punto.getValorMax())
                    .fechas(fechas)
                    .valores(valores)
                    .lecturasDentroRango(dentroRango)
                    .lecturasFueraRango(fueraRango)
                    .build());
        }

        return resultado;
    }

    // 🔒 Defensa en profundidad (V33): el sidebar y el guard de rutas del
    // frontend ya ocultan/bloquean Control de Turno si la empresa no lo
    // tiene habilitado, pero eso no impide una llamada directa a la API.
    // SUPER_ADMIN queda exento, igual que con codigoQrHabilitado/
    // codigoEan13Habilitado (ver SecurityUtils).
    private void validarControlTurnoHabilitado() {
        if (!SecurityUtils.esSuperAdmin() && !SecurityUtils.tieneControlTurnoHabilitado()) {
            throw new BusinessException("Control de Turno no está habilitado para su empresa");
        }
    }

    private void validarRequest(LecturaControlRequest request) {

        if (request.getPuntoControlId() == null) {
            throw new BusinessException("Debe indicar el punto de control");
        }

        if (request.getValor() == null) {
            throw new BusinessException("Debe indicar el valor de la lectura");
        }

        if (request.getTurno() == null) {
            throw new BusinessException("Debe indicar el turno");
        }
    }

    private LecturaControlResponse mapResponse(LecturaControl l) {
        return LecturaControlResponse.builder()
                .id(l.getId())
                .puntoControlId(l.getPuntoControl().getId())
                .puntoControlNombre(l.getPuntoControl().getNombre())
                .unidad(l.getPuntoControl().getUnidad())
                .valor(l.getValor())
                .fechaHora(l.getFechaHora())
                .turno(l.getTurno())
                .observacion(l.getObservacion())
                .usuarioNombre(l.getUsuario() != null ? l.getUsuario().getNombre() : null)
                .build();
    }
}
