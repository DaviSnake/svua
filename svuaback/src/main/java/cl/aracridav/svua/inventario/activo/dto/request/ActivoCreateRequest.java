package cl.aracridav.svua.inventario.activo.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import cl.aracridav.svua.shared.enums.EstadoActivo;
import cl.aracridav.svua.shared.json.LenientLocalDateDeserializer;
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

    // 🔒 Si la fecha viene vacía o con un formato inválido, se usa la fecha
    // actual del servidor en vez de rechazar toda la petición (ver
    // LenientLocalDateDeserializer).
    @JsonDeserialize(using = LenientLocalDateDeserializer.class)
    private LocalDate fechaAdquisicion;
    private BigDecimal valorAdquisicion;
    private BigDecimal valorResidual;
    private Integer vidaUtilMeses;

    private EstadoActivo estadoActual;
    private String cuentaContable;

    private Long ubicacionId;
    private Long proveedorId;
    private Long empresaId;

}
