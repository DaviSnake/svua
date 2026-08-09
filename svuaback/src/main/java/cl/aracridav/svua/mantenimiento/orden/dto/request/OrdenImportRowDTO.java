package cl.aracridav.svua.mantenimiento.orden.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import lombok.Data;

/**
 * Fila de Orden de Mantención ingresada manualmente (grilla tipo planilla)
 * en Carga Masiva. Mismas columnas que el importador de Excel (ver
 * ExcelImportServiceImpl#mapOrden), con fecha y hora ya separadas porque
 * así se ingresan en la grilla (inputs date/time independientes).
 */
@Data
public class OrdenImportRowDTO {

    private String titulo;
    private LocalDate fechaProgramada;
    private LocalTime horaProgramada;
    private Integer duracionMinutos;
    private String tipoMantenimiento; // PREVENTIVO / CORRECTIVO / PREDICTIVO
    private String estado; // PENDIENTE / PROGRAMADA / EN_EJECUCION / PRE_COMPLETADA / COMPLETADA / CANCELADA / ATRASADA
    private String observaciones;
    private String activoNombre;
    private String proveedorRut;
    private BigDecimal valorHoraProveedor;
    private BigDecimal horasEstimadasProveedor;
    private BigDecimal costoManoObraEstimadasProveedor;

}
