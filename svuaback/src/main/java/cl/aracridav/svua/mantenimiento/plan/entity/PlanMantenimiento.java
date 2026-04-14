package cl.aracridav.svua.mantenimiento.plan.entity;

import java.time.LocalDateTime;

import cl.aracridav.svua.inventario.activo.entity.Activo;
import cl.aracridav.svua.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "plan_mantenimiento")
public class PlanMantenimiento extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_plan")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_mantenimiento", nullable = false)
    private TipoMantenimiento tipoMantenimiento;

    @Column(name = "frecuencia_dias", nullable = false)
    private Integer frecuenciaDias;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "esta_activo", nullable = false)
    private Boolean estaActivo;

    @Column(name = "ultima_ejecucion")
    private LocalDateTime ultimaEjecucion;

    @Column(name = "proxima_ejecucion")
    private LocalDateTime proximaEjecucion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_activo", nullable = false, updatable = false)
    private Activo activo;
}
