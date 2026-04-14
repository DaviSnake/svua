package cl.aracridav.svua.inventario.activo.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import cl.aracridav.svua.shared.enums.EstadoActivo;
import lombok.Data;

@Data
public class ActivoCreateRequest {

    private String codigoInterno;
    private String nombre;
    private String descripcion;

    private Long tipoActivoId;

    private String marca;
    private String modelo;
    private String numeroSerie;

    private LocalDate fechaAdquisicion;
    private BigDecimal valorAdquisicion;
    private BigDecimal valorResidual;
    private Integer vidaUtilMeses;

    private EstadoActivo estadoActual;

    private Long ubicacionId;
    private Long proveedorId;
    private Long empresaId;

}
