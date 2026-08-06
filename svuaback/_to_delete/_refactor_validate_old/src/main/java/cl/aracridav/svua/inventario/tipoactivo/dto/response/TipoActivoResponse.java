package cl.aracridav.svua.inventario.tipoactivo.dto.response;

import cl.aracridav.svua.shared.dto.response.EmpresaDTO;
import lombok.*;

@Data
@Builder
public class TipoActivoResponse {

    private Long id;
    private String nombre;
    private String descripcion;
    private Integer vidaUtilReferencialMeses;
    private Boolean activo;
    private EmpresaDTO empresa;

}
