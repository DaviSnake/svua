package cl.aracridav.svua.mantenimiento.ordenrepuesto.entity;

import java.math.BigDecimal;

import cl.aracridav.svua.mantenimiento.orden.entity.OrdenMantenimiento;
import cl.aracridav.svua.mantenimiento.repuesto.entity.Repuesto;
import cl.aracridav.svua.shared.entity.BaseEntity;
import cl.aracridav.svua.usuario.entity.Usuario;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@Table(name = "orden_repuesto")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenRepuesto extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_orden_repuesto")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_orden", nullable = false, updatable = false)
    private OrdenMantenimiento orden;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_repuesto", nullable = false, updatable = false)
    private Repuesto repuesto;

    private Integer cantidad;

    private BigDecimal costoUnitario;

    private BigDecimal costoTotal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false, updatable = false)
    private Usuario usuario;

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (!(o instanceof OrdenRepuesto other)) {
            return false;
        }

        if (id == null || other.id == null) {
            return false;
        }

        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id != null
            ? id.hashCode()
            : System.identityHashCode(this);
    }
}
