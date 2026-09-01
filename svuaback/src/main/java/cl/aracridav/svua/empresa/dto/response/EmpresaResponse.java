package cl.aracridav.svua.empresa.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import cl.aracridav.svua.empresa.entity.TipoPlan;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmpresaResponse {

    private Long id;
    private String nombre;
    private String rut;
    private String emailContacto;
    private String telefono;
    private String direccion;
    private Boolean activa;

    private TipoPlan tipoPlan;
    private LocalDate fechaInicioPlan;
    private LocalDate fechaFinPlan;
    private Integer maxUsuarios;
    private Integer maxActivos;

    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    private Boolean demo;
    private Boolean codigoQrHabilitado;
    private Boolean codigoEan13Habilitado;
    private Boolean controlTurnoHabilitado;
    private Boolean hojaControlHabilitado;
    private Boolean informeMantencionesHabilitado;

    private String colorPrimario;
    // 🔥 No se expone logoRutaArchivo (path en disco): solo si existe o
    // no, igual que OrdenMantenimientoResponse.tieneChecklist. El logo
    // en si se sirve por GET /public/empresas/{id}/logo.
    private boolean tieneLogo;
}
