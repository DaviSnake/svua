package cl.aracridav.svua.notificacion.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import cl.aracridav.svua.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "notificacion")
public class Notificacion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;

    @Column(length = 1000)
    private String mensaje;

    @Column(nullable = false)
    private Boolean leida;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_notificacion")
    private TipoNotificacion tipoNotificacion;

    @CreationTimestamp
    private LocalDateTime fechaCreacion;

}
