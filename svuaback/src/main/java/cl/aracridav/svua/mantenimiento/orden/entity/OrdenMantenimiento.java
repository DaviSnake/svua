package cl.aracridav.svua.mantenimiento.orden.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import cl.aracridav.svua.inventario.activo.entity.Activo;
import cl.aracridav.svua.mantenimiento.ordenrepuesto.entity.OrdenRepuesto;
import cl.aracridav.svua.mantenimiento.plan.entity.PlanMantenimiento;
import cl.aracridav.svua.mantenimiento.plan.entity.TipoMantenimiento;
import cl.aracridav.svua.shared.entity.BaseEntity;
import cl.aracridav.svua.usuario.entity.Usuario;
import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = "fecha_programada", nullable = false)
    private LocalDateTime fechaProgramada;

    @Column(name = "fecha_ejecucion")
    private LocalDateTime fechaEjecucion;

    @Column(name = "fecha_fin_ejecucion")
    private LocalDateTime fechaFinEjecucion;

    @Column(name = "duracion_segundos")
    private Long duracionSegundos;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_mantenimiento", nullable = false)
    private TipoMantenimiento tipoMantenimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoOrden estado;

    @Column(precision = 15, scale = 2)
    private BigDecimal costo;

    @Column(length = 255)
    private String titulo;

    @Column(length = 255)
    private String observaciones;

    // 🔥 NUEVO: quién ejecuta (inicia)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_ejecucion")
    private Usuario usuarioEjecucion;

    // 🔥 NUEVO: quién finaliza (detiene o cancela)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_finalizacion")
    private Usuario usuarioFinalizacion;

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL)
    private List<OrdenRepuesto> repuestosUtilizados;

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Archivo> archivos;

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
private List<OrdenReprogramacion> reprogramaciones;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_activo", nullable = false, updatable = false)
    private Activo activo;

    // 👇 este usuario ahora representa el creador de la orden
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false, updatable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_plan", nullable = false, updatable = false)
    private PlanMantenimiento planMantenimiento;
}
