package cl.aracridav.svua.depreciacion.service;

import java.util.List;

import cl.aracridav.svua.depreciacion.entity.DepreciacionMensual;
import cl.aracridav.svua.depreciacion.entity.TipoDepreciacion;
import cl.aracridav.svua.inventario.activo.entity.Activo;

public interface DepreciacionService {

    public void guardarDepreciacion(Activo activo);

    public void calcularYGuardarDepreciacionMensual(Activo activo);

    public void calcularYGuardarDepreciacionMensual(Activo activo, Long empresaId);

    public List<DepreciacionMensual> obtenerDepreciacionesPorActivo(Activo activo);

    public List<DepreciacionMensual> obtenerDepreciacionesPorActivo(Activo activo, TipoDepreciacion tipo);

    // Backfill: genera el cronograma ACELERADA para los activos de la
    // empresa actual que aún no lo tienen (creados antes de que
    // existiera, o cargados por importación masiva). Idempotente.
    // Devuelve la cantidad de activos procesados.
    public int generarDepreciacionAceleradaFaltante();

    // Genera el cronograma ACELERADA para un activo puntual. Falla si
    // el activo ya lo tiene calculado (evita duplicarlo).
    public void generarDepreciacionAceleradaPorActivo(Long activoId);

}
