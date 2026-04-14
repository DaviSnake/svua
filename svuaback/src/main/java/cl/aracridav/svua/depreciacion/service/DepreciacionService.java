package cl.aracridav.svua.depreciacion.service;

import java.util.List;

import cl.aracridav.svua.depreciacion.entity.DepreciacionMensual;
import cl.aracridav.svua.inventario.activo.entity.Activo;

public interface DepreciacionService {

    public void guardarDepreciacion(Activo activo);
    
    public void calcularYGuardarDepreciacionMensual(Activo activo);

    public List<DepreciacionMensual> obtenerDepreciacionesPorActivo(Activo activo);

}
