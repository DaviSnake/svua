package cl.aracridav.svua.shared.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resultado global genérico de una carga manual (grilla tipo planilla):
 * cuántas filas se guardaron correctamente y el detalle fila por fila.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImportBatchResultDTO {

    private int total;
    private int exitosos;
    private int fallidos;
    private List<ImportRowResultDTO> resultados;

}
