package cl.aracridav.svua.shared.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resultado genérico del procesamiento de una fila individual dentro de una
 * carga manual (grilla tipo planilla), reutilizable para cualquier entidad
 * de Carga Masiva (Proveedor, Orden, Repuesto, Ubicación, Tipo de Activo).
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImportRowResultDTO {

    private int fila;
    private boolean exito;
    private String mensaje;
    private String referencia; // código / nombre / título / rut, según la entidad
    private Long id;

}
