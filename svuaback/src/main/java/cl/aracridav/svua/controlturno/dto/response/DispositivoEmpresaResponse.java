package cl.aracridav.svua.controlturno.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DispositivoEmpresaResponse {

    private Long id;
    private String codigoDispositivo;
    private String descripcion;
    private Boolean activo;
    private Long empresaId;
    private String empresaNombre;
}
