package cl.aracridav.svua.inventario.ubicacion.dto.request;

import lombok.Data;

/**
 * Fila de Ubicación ingresada manualmente (grilla tipo planilla) en Carga Masiva.
 * Mismas columnas que el importador de Excel (ver ExcelImportServiceImpl#mapUbicacion).
 */
@Data
public class UbicacionImportRowDTO {

    private String nombre;
    private String descripcion;
    private String direccion;

}
