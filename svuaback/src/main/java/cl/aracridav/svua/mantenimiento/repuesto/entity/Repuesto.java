package cl.aracridav.svua.mantenimiento.repuesto.entity;

import java.math.BigDecimal;

import cl.aracridav.svua.shared.entity.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.envers.Audited;
import lombok.*;
import org.hibernate.annotations.BatchSize;

@Getter
@Setter
@Entity
@Audited
@Table(name = "repuesto")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@BatchSize(size = 20)
public class Repuesto extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_repuesto")
    private Long id;

    // 🔐 Optimistic locking: protege stockActual contra "lost update"
    // cuando dos operaciones concurrentes (ej. dos ordenes de
    // mantenimiento distintas usando el mismo repuesto) leen, restan y
    // guardan el stock casi al mismo tiempo. Hibernate incrementa esta
    // columna en cada UPDATE y rechaza el segundo save() con
    // ObjectOptimisticLockingFailureException si la version ya cambio.
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(nullable = false)
    private String codigo;

    @Column(nullable = false)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal costoUnitario;

    @Column(name = "stock_actual", nullable = false)
    private Integer stockActual;

    @Column(nullable = false)
    private Integer stockMinimo;

    @Column(nullable = false)
    private Boolean activo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoRepuesto tipo;

    @Column(name = "cuenta_contable")
    private String cuentaContable;

}
