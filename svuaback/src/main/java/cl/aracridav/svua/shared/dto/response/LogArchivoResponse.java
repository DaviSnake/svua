package cl.aracridav.svua.shared.dto.response;

import java.time.LocalDateTime;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogArchivoResponse {

    private Long empresaId;
    private String nombreEmpresa;
    private String nombreArchivo;
    private Long tamanioBytes;
    private LocalDateTime fechaCreacion;
}
