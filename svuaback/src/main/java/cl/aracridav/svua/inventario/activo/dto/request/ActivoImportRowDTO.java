package cl.aracridav.svua.inventario.activo.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import cl.aracridav.svua.shared.json.LenientLocalDateDeserializer;
import lombok.Data;

/**
 * Representa una fila de Activo ingresada manualmente (grilla tipo planilla)
 * en el modo "Ingresar manualmente" de Carga Masiva.
 *
 * Tiene el mismo significado de columnas que el importador de Excel
 * (ver ExcelImportServiceImpl#mapActivo), pero ya tipado, porque llega
 * como JSON desde el frontend en vez de celdas de Apache POI.
 */
@Data
public class ActivoImportRowDTO {

    private String codigoInterno;
    private String nombre;
    private String descripcion;
    private String tipoActivoNombre;
    private String marca;
    private String modelo;
    private String numeroSerie;

    // 🔒 Si la fecha viene vacía o con un formato inválido, se usa la fecha
    // actual del servidor en vez de rechazar toda la fila (ver
    // LenientLocalDateDeserializer).
    @JsonDeserialize(using = LenientLocalDateDeserializer.class)
    private LocalDate fechaAdquisicion;
    private BigDecimal valorAdquisicion;
    private BigDecimal valorResidual;
    private Integer vidaUtilMeses;
    private String ubicacionNombre;
    private String proveedorRut;
    private String cuentaContable;

}
