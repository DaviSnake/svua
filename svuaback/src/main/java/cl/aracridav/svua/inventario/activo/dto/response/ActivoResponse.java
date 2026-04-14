package cl.aracridav.svua.inventario.activo.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import cl.aracridav.svua.shared.dto.response.EmpresaDTO;
import cl.aracridav.svua.shared.enums.EstadoActivo;
import lombok.*;

@Data
@Builder
@JsonPropertyOrder({
    "id",
    "codigoInterno",
    "nombre",
    "descripcion",
    "marca",
    "modelo",
    "numeroSerie",
    "fechaAdquisicion",
    "valorAdquisicion",
    "vidaUtilMeses",
    "estadoActual",
    "tipoActivo",
    "ubicacion",
    "proveedor",
    "empresa"
})
public class ActivoResponse {

    private Long id;
    private String codigoInterno;
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
    
    private TipoActivoDTO tipoActivo;

    private UbicacionDTO ubicacion;

    private ProveedorDTO proveedor;

    private EmpresaDTO empresa;

}
