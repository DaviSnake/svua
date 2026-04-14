package cl.aracridav.svua.empresa.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Data
@Table(name = "empresa")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "empresa_id")
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, unique = true, length = 20)
    private String rut;

    @Column(length = 150)
    private String emailContacto;

    @Column(length = 30)
    private String telefono;

    @Column(length = 255)
    private String direccion;

    @Column(nullable = false)
    private Boolean activa;

    // 🔹 Control de plan SaaS
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoPlan tipoPlan;

    @Column(name = "fecha_inicio_plan")
    private LocalDate fechaInicioPlan;

    @Column(name = "fecha_fin_plan")
    private LocalDate fechaFinPlan;

    @Column(name = "max_usuarios")
    private Integer maxUsuarios;

    @Column(name = "max_activos")
    private Integer maxActivos;

    // 🔹 Auditoría
    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
