package cl.aracridav.svua.notificacion.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.empresa.repository.EmpresaRepository;
import cl.aracridav.svua.mantenimiento.repuesto.entity.Repuesto;
import cl.aracridav.svua.notificacion.entity.Notificacion;
import cl.aracridav.svua.notificacion.entity.TipoNotificacion;
import cl.aracridav.svua.notificacion.entity.TipoReferencia;
import cl.aracridav.svua.notificacion.repository.NotificacionRepository;
import cl.aracridav.svua.shared.exception.BusinessException;
import cl.aracridav.svua.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificacionServiceImpl implements NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final EmpresaRepository empresaRepository;

    /*
     * =========================================
     * VERIFICAR STOCK MINIMO
     * =========================================
     */
    @Override
    @Transactional
    public void verificarStockMinimo(Repuesto repuesto) {

        Empresa empresa = obtenerEmpresaActual();

        Optional<Notificacion> notificacionExistente =
        notificacionRepository
                .findByReferenciaIdAndTipoReferenciaAndTipoNotificacion(
                        repuesto.getId(),
                        TipoReferencia.REPUESTO,
                        TipoNotificacion.STOCK_BAJO
                );

        if (repuesto.getStockActual() <= repuesto.getStockMinimo()) {

            if (notificacionExistente.isEmpty()) {

                Notificacion notificacion = new Notificacion();
                notificacion.setReferenciaId(repuesto.getId());
                notificacion.setTipoNotificacion(TipoNotificacion.STOCK_BAJO);
                notificacion.setTitulo("Stock bajo");
                notificacion.setMensaje(
                        "El producto " + repuesto.getNombre()
                                + " alcanzó el stock mínimo."
                );
                notificacion.setLeida(false);
                notificacion.setTipoReferencia(TipoReferencia.REPUESTO);
                notificacion.setFechaCreacion(LocalDateTime.now());
                notificacion.setEmpresa(empresa);

                notificacionRepository.save(notificacion);
            }

        } else {

            notificacionExistente.ifPresent(
                    notificacionRepository::delete
            );
        }
    }

    private Empresa obtenerEmpresaActual() {
        return empresaRepository.findById(SecurityUtils.getEmpresaId())
                .orElseThrow(() -> new BusinessException("Empresa no encontrada"));
    }


}
