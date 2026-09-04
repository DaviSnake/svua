package cl.aracridav.svua.controlturno.dto.request;

import lombok.Data;

@Data
public class DispositivoEmpresaRequest {

    private String codigoDispositivo;
    private String descripcion;
    private Long empresaId;
}
