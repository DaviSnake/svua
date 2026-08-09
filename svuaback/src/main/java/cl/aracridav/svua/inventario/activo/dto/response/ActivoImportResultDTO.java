package cl.aracridav.svua.inventario.activo.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resultado global de una carga manual (grilla tipo planilla) de Activos:
 * cuántas filas se guardaron correctamente y el detalle fila por fila.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ActivoImportResultDTO {

    private int total;
    private int exitosos;
    private int fallidos;
    private List<ActivoImportRowResultDTO> resultados;

}
