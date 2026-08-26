package cl.aracridav.svua.usuario.entity;

import java.time.LocalDateTime;
import java.util.List;

import cl.aracridav.svua.mantenimiento.orden.entity.OrdenMantenimiento;
import cl.aracridav.svua.shared.entity.BaseEntity;
import cl.aracridav.svua.shared.enums.RolUsuario;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

// 🔎 Historial de cambios (NCh-ISO/IEC 27001 A.12) via Hibernate Envers
// -- ver revision_info/usuario_aud (migracion V30). password,
// intentosFallidos y fechaBloqueo quedan @NotAudited a proposito: el
// hash de la contrasena no aporta nada auditable (no es legible) y solo
// suma superficie de exposicion innecesaria, e intentosFallidos/
// fechaBloqueo cambian en CADA intento de login fallido -- auditarlos
// generaria una revision nueva por cada intento fallido, sin valor real
// (ese conteo ya se puede ver en el estado actual del usuario, no hace
// falta su historico).
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Audited
@Table(
    name = "usuario",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "email")
    }
)
@BatchSize(size = 20)
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

    @NotAudited
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private Boolean activo;

    @NotAudited
    private Integer intentosFallidos;

    @NotAudited
    private LocalDateTime fechaBloqueo;

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL)
    private List<OrdenMantenimiento> ordenes;
}
