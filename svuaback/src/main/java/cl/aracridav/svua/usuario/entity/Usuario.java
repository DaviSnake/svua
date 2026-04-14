package cl.aracridav.svua.usuario.entity;

import java.time.LocalDateTime;
import java.util.List;

import cl.aracridav.svua.mantenimiento.orden.entity.OrdenMantenimiento;
import cl.aracridav.svua.shared.entity.BaseEntity;
import cl.aracridav.svua.shared.enums.RolUsuario;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
    name = "usuario",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "email")
    }
)
public class Usuario extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RolUsuario rol;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private Boolean activo;

    private Integer intentosFallidos;

    private LocalDateTime fechaBloqueo;

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL)
    private List<OrdenMantenimiento> ordenes;
}
