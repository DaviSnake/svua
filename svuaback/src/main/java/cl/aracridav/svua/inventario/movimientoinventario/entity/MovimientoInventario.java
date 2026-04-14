package cl.aracridav.svua.inventario.movimientoinventario.entity;

import java.time.LocalDateTime;

import cl.aracridav.svua.inventario.bodega.entity.Bodega;
import cl.aracridav.svua.mantenimiento.repuesto.entity.Repuesto;
import cl.aracridav.svua.shared.entity.BaseEntity;
import cl.aracridav.svua.usuario.entity.Usuario;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Table(name = "movimiento_inventario")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoInventario extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimiento_inventario")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_stock_repuesto", nullable = false, updatable = false)
    private Repuesto repuesto;

    @Enumerated(EnumType.STRING)
    private TipoMovimiento tipo;

    private Integer cantidad;

    private Integer stockAnterior;

    private Integer stockPosterior;

    private LocalDateTime fecha;

    private String referencia; // Orden, compra, ajuste

    private String motivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_bodega")
    private Bodega bodega;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

}
