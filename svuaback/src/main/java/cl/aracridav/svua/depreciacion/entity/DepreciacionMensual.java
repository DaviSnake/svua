package cl.aracridav.svua.depreciacion.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import cl.aracridav.svua.inventario.activo.entity.Activo;
import cl.aracridav.svua.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "depreciacion_mensual")
public class DepreciacionMensual extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activo_id", nullable = false)
    private Activo activo;

    @Column(nullable = false)
    private int mes;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private BigDecimal depreciacionMensual;

    @Column(nullable = false)
    private BigDecimal depreciacionAcumulada;

    @Column(nullable = false)
    private BigDecimal valorContable;

}
