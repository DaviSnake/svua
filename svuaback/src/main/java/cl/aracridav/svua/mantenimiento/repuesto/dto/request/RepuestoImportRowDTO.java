package cl.aracridav.svua.mantenimiento.repuesto.dto.request;

import java.math.BigDecimal;

import lombok.Data;

/**
 * Fila de Repuesto ingresada manualmente (grilla tipo planilla) en Carga Masiva.
 * Mismas columnas que el importador de Excel (ver ExcelImportServiceImpl#mapRepuesto).
 */
@Data
public class RepuestoImportRowDTO {

    private String codigo;
    private String nombre;
    private String descripcion;
    private BigDecimal costo;
    private Integer stockActual;
    private Integer stockMinimo;
    private String cuentaContable;
    private String tipoRepuesto; // REPUESTO / FUNGIBLE

}
