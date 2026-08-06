package cl.aracridav.svua.usuario.entity;

import java.time.LocalDateTime;

import cl.aracridav.svua.shared.entity.BaseEntity;
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
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "sesion_usuario")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SesionUsuario extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(name = "fecha_login")
    private LocalDateTime fechaLogin;

    @Column(name = "ultima_actividad")
    private LocalDateTime ultimaActividad;

    @Column(name = "fecha_logout")
    private LocalDateTime fechaLogout;

    @Column(name = "pagina_actual")
    private String paginaActual;

    @Column(name = "ip")
    private String ip;

    @Column(name = "navegador")
    private String navegador;

    @Column(name = "sistema_operativo")
    private String sistemaOperativo;

    @Column(name = "activa")
    private Boolean activa;

    @Column(name = "cantidad_requests")
    private Integer cantidadRequests;

    @Column(name = "ultima_accion")
    private String ultimaAccion;

    @Column(name = "version_app")
    private String versionApp;

    @Column(name = "dispositivo")
    private String dispositivo;

    @Column(name = "token_jti")
    private String tokenJti;
}
