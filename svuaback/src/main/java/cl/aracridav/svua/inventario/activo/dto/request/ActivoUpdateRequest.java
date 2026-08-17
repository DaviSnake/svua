package cl.aracridav.svua.inventario.activo.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

import cl.aracridav.svua.shared.enums.EstadoActivo;

@Data
public class ActivoUpdateRequest {

    // 🔒 El codigo interno ya no se puede modificar desde el mantenedor
    // (queda fijo desde la creacion, junto con el QR/EAN13 que se
    // generan a partir de el). Por eso no existe aqui.
    private String nombre;
    private String descripcion;

    private String marca;
    private String modelo;
    private String numeroSerie;

    private LocalDate fechaAdquisicion;

    private BigDecimal valorAdquisicion;
    private BigDecimal valorResidual;
    private Integer vidaUtilMeses;

    private EstadoActivo estadoActual;
    private String cuentaContable;

    // 🔗 Relaciones
    private Long tipoActivoId;
    private Long ubicacionId;
    private Long proveedorId;
}