package cl.aracridav.svua.proveedor.dto.request;

import lombok.Data;

/**
 * Fila de Proveedor ingresada manualmente (grilla tipo planilla) en Carga Masiva.
 * Mismas columnas que el importador de Excel (ver ExcelImportServiceImpl#mapProveedor).
 */
@Data
public class ProveedorImportRowDTO {

    private String nombre;
    private String rut;
    private String contacto;
    private String telefono;
    private String email;
    private String tipoProveedor; // INTERNO / EXTERNO

}
