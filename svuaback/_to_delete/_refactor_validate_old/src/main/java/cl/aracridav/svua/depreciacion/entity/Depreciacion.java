package cl.aracridav.svua.depreciacion.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

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
@Table(name = "depreciacion")
public class Depreciacion extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_depreciacion")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MetodoDepreciacion metodo;

    @Column(
        name = "valor_inicial",
        nullable = false,
        precision = 15,
        scale = 2
    )
    private BigDecimal valorInicial;

    @Column(
        name = "valor_residual",
        nullable = false,
        precision = 15,
        scale = 2
    )
    private BigDecimal valorResidual;

    @Column(name = "vida_util_meses", nullable = false)
    private Integer vidaUtilMeses;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_activo", nullable = false, updatable = false)
    private Activo activo;
}
