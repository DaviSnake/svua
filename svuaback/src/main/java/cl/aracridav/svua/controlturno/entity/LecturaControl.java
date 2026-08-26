package cl.aracridav.svua.controlturno.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import cl.aracridav.svua.controlturno.enums.TurnoTrabajo;
import cl.aracridav.svua.shared.entity.BaseEntity;
import cl.aracridav.svua.usuario.entity.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 🔥 Lectura horaria de un PuntoControl, registrada manualmente por el
// tecnico/jefe de turno (ej. temperatura de un horno a las 14:00 del
// turno TARDE). Es la fuente de datos de los graficos de
// Control de Turno (ver LecturaControlServiceImpl.dashboard).
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "lectura_control")
public class LecturaControl extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_lectura_control")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "punto_control_id", nullable = false)
    private PuntoControl puntoControl;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    // 🔥 Fecha/hora real de la medicion (puede diferir de fechaCreacion,
    // heredada de BaseEntity, que es cuando se guarda el registro en el
    // sistema).
    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TurnoTrabajo turno;

    @Column(columnDefinition = "TEXT")
    private String observacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
}
