package cl.aracridav.svua.inventario.tipoactivo.dto.request;

import lombok.Data;

/**
 * Fila de Tipo de Activo ingresada manualmente (grilla tipo planilla) en Carga Masiva.
 * Mismas columnas que el importador de Excel (ver ExcelImportServiceImpl#mapTipoActivo).
 */
@Data
public class TipoActivoImportRowDTO {

    private String nombre;
    private String descripcion;
    private Integer vidaUtilReferencialMeses;

}
