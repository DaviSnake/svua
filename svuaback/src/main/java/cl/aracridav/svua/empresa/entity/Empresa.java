package cl.aracridav.svua.empresa.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.BatchSize;

@Entity
@Data
@Table(name = "empresa")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@BatchSize(size = 20)
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "empresa_id")
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, unique = true, length = 20)
    private String rut;

    @Column(length = 150)
    private String emailContacto;

    @Column(length = 30)
    private String telefono;

    @Column(length = 255)
    private String direccion;

    @Column(nullable = false)
    private Boolean activa;

    // 🔹 Control de plan SaaS
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoPlan tipoPlan;

    @Column(name = "fecha_inicio_plan")
    private LocalDate fechaInicioPlan;

    @Column(name = "fecha_fin_plan")
    private LocalDate fechaFinPlan;

    @Column(name = "max_usuarios")
    private Integer maxUsuarios;

    @Column(name = "max_activos")
    private Integer maxActivos;

    // 🔹 Auditoría
    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(nullable = false)
    private Boolean demo;

    // 🔹 Habilitan, por empresa, la generacion/uso de codigo QR y codigo
    // EAN13 para sus activos (ver ActivoServiceImpl.ocultarCodigosSiNoCorresponde
    // y buscarPorCodigoEscaneado).
    @Column(name = "codigo_qr_habilitado", nullable = false)
    private Boolean codigoQrHabilitado;

    @Column(name = "codigo_ean13_habilitado", nullable = false)
    private Boolean codigoEan13Habilitado;

    // 🔹 Habilita, por empresa, el modulo Control de Turno (catalogo de
    // puntos de control + registro de lecturas + dashboard). Mismo
    // patron que codigoQrHabilitado/codigoEan13Habilitado: antes el
    // unico criterio de acceso era el rol del usuario, sin distincion
    // por empresa (ver V33, sidebar.component.ts, SecurityUtils).
    @Column(name = "control_turno_habilitado", nullable = false)
    private Boolean controlTurnoHabilitado;

    // 🔹 Habilita, por empresa, el boton "Importar Excel (HOJA DE
    // CONTROL)" dentro de Control de Turno (ver
    // HojaControlImportServiceImpl): el parser es especifico al layout
    // de una planilla real de una empresa puntual, no generico. Mismo
    // patron que controlTurnoHabilitado.
    @Column(name = "hoja_control_habilitado", nullable = false)
    private Boolean hojaControlHabilitado;

    // 🔹 Habilita, por empresa, el "Informe de Mantenciones" (ver
    // OrdenMantenimientoController.obtenerInformeMantenciones). Mismo
    // patron que controlTurnoHabilitado/hojaControlHabilitado: opt-in
    // (FALSE por defecto para empresas nuevas, ver
    // construirEmpresaBase). V37 agrego la columna en DEFAULT TRUE a
    // proposito, para no quitarsela de un dia para otro a empresas que
    // ya la tenian sin restriccion -- esos datos existentes NO se
    // tocan. V40 corrigio el DEFAULT de la columna a FALSE (para
    // empresas nuevas de ahi en adelante).
    @Column(name = "informe_mantenciones_habilitado", nullable = false)
    private Boolean informeMantencionesHabilitado;

    // 🔹 Personalizacion por empresa (V38): logo propio (reemplaza el
    // logo generico del sidebar) y color de acento. Ambos opcionales --
    // si vienen null, el frontend usa el logo/color por defecto.
    @Column(name = "logo_ruta_archivo", length = 500)
    private String logoRutaArchivo;

    @Column(name = "color_primario", length = 7)
    private String colorPrimario;
}
