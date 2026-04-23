package cl.aracridav.svua.mantenimiento.orden.entity;

import java.time.LocalDateTime;

import cl.aracridav.svua.shared.entity.BaseEntity;
import cl.aracridav.svua.usuario.entity.Usuario;
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
@Table(name = "orden_reprogramacion")
public class OrdenReprogramacion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Orden afectada
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_orden", nullable = false)
    private OrdenMantenimiento orden;

    // Fecha antes del cambio
    @Column(name = "fecha_anterior", nullable = false)
    private LocalDateTime fechaAnterior;

    // Nueva fecha programada
    @Column(name = "fecha_nueva", nullable = false)
    private LocalDateTime fechaNueva;

    // Quién hizo la reprogramación
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    // Opcional: motivo
    @Column(length = 255)
    private String motivo;
}
