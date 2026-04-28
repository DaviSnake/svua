package cl.aracridav.svua.shared.dto.response;

import java.util.ArrayList;
import java.util.List;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImportProgressDTO {
    private int total;
    private int procesados;
    private int errores;
    private String estado; // PROCESANDO, COMPLETADO, ERROR

    @Builder.Default
    private List<ErrorFilaDTO> erroresDetalle = new ArrayList<>(); // 🔥 clave
}
