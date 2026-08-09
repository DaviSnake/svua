package cl.aracridav.svua.inventario.activo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resultado del procesamiento de una fila individual dentro de una carga
 * manual (grilla tipo planilla) de Activos.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ActivoImportRowResultDTO {

    private int fila;
    private boolean exito;
    private String mensaje;
    private String codigoInterno;
    private Long activoId;

}
