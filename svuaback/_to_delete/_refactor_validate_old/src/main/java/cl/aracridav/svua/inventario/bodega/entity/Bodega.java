package cl.aracridav.svua.inventario.bodega.entity;

import cl.aracridav.svua.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "bodega")
public class Bodega extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_bodega")
    private Long id;

    private String nombre;

    private String ubicacionFisica;

    @Column(nullable = false)
    private Boolean activa;

}
