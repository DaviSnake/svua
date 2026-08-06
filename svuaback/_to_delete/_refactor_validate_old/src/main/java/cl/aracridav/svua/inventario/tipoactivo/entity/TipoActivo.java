package cl.aracridav.svua.inventario.tipoactivo.entity;

import cl.aracridav.svua.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tipo_activo")
public class TipoActivo extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_activo")
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "vida_util_referencial_meses", nullable = false)
    private Integer vidaUtilReferencialMeses;

    @Column(nullable = false)
    private Boolean activo;
}
