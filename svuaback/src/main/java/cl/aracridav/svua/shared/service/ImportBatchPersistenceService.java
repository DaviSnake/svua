package cl.aracridav.svua.shared.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import cl.aracridav.svua.depreciacion.service.DepreciacionService;
import cl.aracridav.svua.inventario.activo.entity.Activo;
import cl.aracridav.svua.inventario.activo.repository.ActivoRepository;
import cl.aracridav.svua.inventario.historial.service.HistorialEstadoActivoService;
import cl.aracridav.svua.inventario.tipoactivo.entity.TipoActivo;
import cl.aracridav.svua.inventario.tipoactivo.repository.TipoActivoRepository;
import cl.aracridav.svua.inventario.ubicacion.entity.Ubicacion;
import cl.aracridav.svua.inventario.ubicacion.repository.UbicacionRepository;
import cl.aracridav.svua.mantenimiento.orden.entity.OrdenMantenimiento;
import cl.aracridav.svua.mantenimiento.orden.repository.OrdenMantenimientoRepository;
import cl.aracridav.svua.mantenimiento.repuesto.entity.Repuesto;
import cl.aracridav.svua.mantenimiento.repuesto.repository.RepuestoRepository;
import cl.aracridav.svua.proveedor.entity.Proveedor;
import cl.aracridav.svua.proveedor.repository.ProveedorRepository;
import cl.aracridav.svua.shared.enums.EstadoActivo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;

/**
 * 🔐 Persiste cada LOTE (batch) de la carga masiva asincrona (Excel, ver
 * ExcelImportServiceImpl.procesarAsync) en su PROPIA transaccion fisica,
 * independiente de las demas.
 *
 * Antes, procesarAsync() corria bajo un unico @Transactional que
 * envolvia TODO el archivo (miles de filas): si una fila fallaba a
 * mitad de camino, Postgres marcaba la transaccion completa como
 * abortada y el COMMIT final de Spring se convertia silenciosamente en
 * ROLLBACK, perdiendo TODO el archivo — mientras el tracker de progreso
 * en memoria (ImportProgressService) ya habia reportado como
 * "procesadas" las filas de los lotes anteriores, dando una falsa
 * sensacion de exito parcial que nunca se guardo.
 *
 * Al extraer el guardado de cada lote a este bean separado con
 * @Transactional(REQUIRES_NEW), cada lote hace su propio commit real e
 * independiente: si un lote posterior falla, los lotes anteriores ya
 * comprometidos NO se revierten.
 *
 * (Nota tecnica: esto no se puede lograr agregando
 * @Transactional(REQUIRES_NEW) a un metodo privado dentro de la misma
 * clase que lo invoca — el proxy AOP de Spring no intercepta llamadas
 * auto-invocadas dentro del mismo bean. De ahi la necesidad de este bean
 * separado.)
 *
 * Los metodos sincronos de carga manual (grilla) en ExcelImportServiceImpl
 * (procesarActivosManual, etc.) siguen usando sus propios helpers
 * privados sin tocar, ya que no dependen de un tracker de progreso en
 * memoria/polling y no sufren este problema.
 */
@Service
@RequiredArgsConstructor
public class ImportBatchPersistenceService {

    private final ActivoRepository activoRepository;
    private final ProveedorRepository proveedorRepository;
    private final OrdenMantenimientoRepository oMantenimientoRepository;
    private final RepuestoRepository repuestoRepository;
    private final UbicacionRepository ubicacionRepository;
    private final TipoActivoRepository tipoActivoRepository;
    private final DepreciacionService depreciacionService;
    private final HistorialEstadoActivoService historialEstadoActivoService;

    @PersistenceContext
    private EntityManager em;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void guardarActivoBatch(List<Activo> batch, Long empresaId, Long usuarioId) {
        List<Activo> activos = activoRepository.saveAll(batch);
        calcularYGuardarDepreciacionMensual(activos, empresaId);
        guardarHistoriaCracionActivo(activos, empresaId, usuarioId);
        em.flush();
        em.clear();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void guardarProveedorBatch(List<Proveedor> batch) {
        proveedorRepository.saveAll(batch);
        em.flush();
        em.clear();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void guardarOrdenBatch(List<OrdenMantenimiento> batch) {
        oMantenimientoRepository.saveAll(batch);
        em.flush();
        em.clear();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void guardarRepuestoBatch(List<Repuesto> batch) {
        repuestoRepository.saveAll(batch);
        em.flush();
        em.clear();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void guardarUbicacionBatch(List<Ubicacion> batch) {
        ubicacionRepository.saveAll(batch);
        em.flush();
        em.clear();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void guardarTipoActivoBatch(List<TipoActivo> batch) {
        tipoActivoRepository.saveAll(batch);
        em.flush();
        em.clear();
    }

    private void calcularYGuardarDepreciacionMensual(List<Activo> activos, Long empresaId) {
        for (Activo activo : activos) {
            depreciacionService.calcularYGuardarDepreciacionMensual(activo, empresaId);
        }
    }

    private void guardarHistoriaCracionActivo(List<Activo> activos, Long empresaId, Long usuarioId) {
        for (Activo activo : activos) {
            historialEstadoActivoService.registrarCambioEstado(
                activo.getId(), EstadoActivo.OPERATIVO, null, "Creación de Activo", usuarioId
            );
        }
    }

}
