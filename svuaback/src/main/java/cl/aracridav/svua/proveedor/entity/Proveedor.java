package cl.aracridav.svua.proveedor.entity;

import cl.aracridav.svua.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
    name = "proveedor",
     uniqueConstraints = {
        @UniqueConstraint(columnNames = "rut")
    }
)
public class Proveedor extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_proveedor")
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 20)
    private String rut;

    @Column(length = 150)
    private String contacto;

    @Column(length = 30)
    private String telefono;

    @Column(length = 150)
    private String email;

    @Column(nullable = false)
    private Boolean activo;
}
