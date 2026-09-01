package cl.aracridav.svua.controlturno.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Data;

// 🔥 Resultado de importar la "HOJA DE CONTROL" (ver
// HojaControlImportServiceImpl): cuantas lecturas nuevas se crearon,
// cuantas ya existian para ese punto+hora (no se duplican), y el
// nombre de los puntos de control que no existian en el catalogo y se
// crearon al vuelo durante esta importación.
@Data
@Builder
public class ImportHojaControlResponse {

    private int lecturasCreadas;
    private int lecturasOmitidas;
    private List<String> puntosNuevosCreados;

}
