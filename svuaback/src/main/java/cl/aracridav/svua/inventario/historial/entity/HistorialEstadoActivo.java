package cl.aracridav.svua.inventario.historial.entity;

import java.time.LocalDateTime;

import cl.aracridav.svua.inventario.activo.entity.Activo;
import cl.aracridav.svua.shared.entity.BaseEntity;
import cl.aracridav.svua.shared.enums.EstadoActivo;
import cl.aracridav.svua.usuario.entity.Usuario;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "historial_estado_activo")
public class HistorialEstadoActivo extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoActivo estado;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(length = 255)
    private String comentario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_activo", nullable = false, updatable = false)
    private Activo activo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false, updatable = false)
    private Usuario usuario;
}
