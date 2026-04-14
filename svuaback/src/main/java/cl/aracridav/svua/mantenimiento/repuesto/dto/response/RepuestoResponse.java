package cl.aracridav.svua.mantenimiento.repuesto.dto.response;

import java.math.BigDecimal;

import cl.aracridav.svua.shared.dto.response.EmpresaDTO;
import lombok.*;

@Data
@Builder
public class RepuestoResponse {

    private Long id;
    private String codigo;
    private String nombre;
    private String descripcion;
    private BigDecimal costoUnitario;
    private Integer stockMinimo;
    private EmpresaDTO empresa;
    private Boolean activo;

}
