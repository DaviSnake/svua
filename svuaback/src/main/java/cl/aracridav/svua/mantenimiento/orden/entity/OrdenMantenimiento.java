package cl.aracridav.svua.mantenimiento.orden.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cl.aracridav.svua.inventario.activo.entity.Activo;
import cl.aracridav.svua.mantenimiento.ordenrepuesto.entity.OrdenRepuesto;
import cl.aracridav.svua.proveedor.entity.Proveedor;
import cl.aracridav.svua.shared.entity.BaseEntity;
import cl.aracridav.svua.usuario.entity.Usuario;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "orden_mantenimiento")
public class OrdenMantenimiento extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_orden")
    private Long id;

    // =========================
    // FECHAS DEL PROCESO
    // =========================

    @Column(name = "fecha_programada", nullable = false)
    private LocalDateTime fechaProgramada;

    @Column(name = "fecha_termino", nullable = false)
    private LocalDateTime fechaTermino;

    @Column(name = "fecha_ejecucion")
    private LocalDateTime fechaEjecucion;

    @Column(name = "fecha_fin_ejecucion")
    private LocalDateTime fechaFinEjecucion;

    // =========================
    // TIEMPO REAL DE EJECUCIÓN
    // =========================

    @Column(name = "duracion_segundos")
    private Long duracionSegundos;

    // =========================
    // TIPO Y ESTADO
    // =========================

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_mantenimiento", nullable = false)
    private TipoMantenimiento tipoMantenimiento;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoOrden estado;

    // =========================
    // INFORMACIÓN GENERAL
    // =========================

    @Column(name = "titulo", length = 255)
    private String titulo;

    @Column(name = "observaciones", length = 255)
    private String observaciones;

    @Column(name = "ruta_archivo")
    private String rutaArchivo;

    // =========================
    // COSTOS INTERNOS (si aplica)
    // =========================

    @Column(name = "costo_total", precision = 15, scale = 2)
    private BigDecimal costoTotal;

    // =========================
    // PROVEEDOR (HH + COSTOS)
    // =========================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_proveedor")
    private Proveedor proveedor;

    @Column(name = "horas_estimadas_proveedor", precision = 15, scale = 2)
    private BigDecimal horasEstimadasProveedor;

    @Column(name = "horas_reales_proveedor", precision = 15, scale = 2)
    private BigDecimal horasRealesProveedor;

    @Column(name = "valor_hora_proveedor", precision = 15, scale = 2)
    private BigDecimal valorHoraProveedor;

    @Column(name = "costo_mano_obra_estimadas_proveedor", precision = 15, scale = 2)
    private BigDecimal costoManoObraEstimadasProveedor;

    @Column(name = "costo_mano_obra_proveedor", precision = 15, scale = 2)
    private BigDecimal costoManoObraProveedor;

    // =========================
    // USUARIOS
    // =========================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_ejecucion")
    private Usuario usuarioEjecucion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_pre_finalizacion")
    private Usuario usuarioPreFinalizacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_finalizacion")
    private Usuario usuarioFinalizacion;

    // creador de la orden
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false, updatable = false)
    private Usuario usuario;

    // =========================
    // RELACIONES OPERATIVAS
    // =========================

    @Builder.Default
    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20)
    private Set<OrdenRepuesto> repuestosUtilizados = new HashSet<>();

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Archivo> archivos;

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrdenReprogramacion> reprogramaciones;

    // =========================
    // CONTEXTO OPERATIVO
    // =========================

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_activo", nullable = false, updatable = false)
    private Activo activo;
}