package cl.aracridav.svua.inventario.bodega.dto.response;

import cl.aracridav.svua.shared.dto.response.EmpresaDTO;
import lombok.*;

@Data
@Builder
public class BodegaResponse {

    private Long id;
    private String nombre;
    private String ubicacionFisica;
    private EmpresaDTO empresa;

}
