package cl.aracridav.svua.mantenimiento.repuesto.entity;

import java.math.BigDecimal;

import cl.aracridav.svua.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@Table(name = "repuesto")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Repuesto extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_repuesto")
    private Long id;

    @Column(nullable = false)
    private String codigo;

    @Column(nullable = false)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal costoUnitario;

    @Column(nullable = false)
    private Integer stockMinimo;

    @Column(nullable = false)
    private Boolean activo;

}
