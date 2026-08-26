package cl.aracridav.svua.shared.auditoria;

import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

// 🔎 Fila de "revision" que Hibernate Envers crea automaticamente en
// cada INSERT/UPDATE/DELETE de una entidad marcada @Audited (ver
// Usuario, Activo, OrdenMantenimiento, etc.). Cada _aud generada por
// Envers (usuario_aud, activo_aud, ...) tiene una FK "rev" hacia esta
// tabla: aca se guarda CUANDO fue el cambio y (via RevisionInfoListener)
// QUIEN lo hizo, en un solo lugar en vez de repetirlo en cada _aud.
//
// No extiende org.hibernate.envers.DefaultRevisionEntity a proposito:
// esa clase usa su propia estrategia de generacion de ID (secuencia de
// Hibernate), que no calzaria con la migracion Flyway V30 (columna
// IDENTITY, igual que el resto de las tablas de este proyecto).
@Getter
@Setter
@Entity
@Table(name = "revision_info")
@RevisionEntity(RevisionInfoListener.class)
public class RevisionInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @RevisionNumber
    @Column(name = "rev")
    private Long id;

    // 🔥 epoch millis (no LocalDateTime): asi lo exige @RevisionTimestamp
    // de Envers. Se puede convertir a fecha legible con
    // Instant.ofEpochMilli(timestamp) donde se necesite mostrar.
    @RevisionTimestamp
    @Column(name = "revtstmp", nullable = false)
    private Long timestamp;

    // Quien hizo el cambio. NULL si el cambio vino de un scheduler/job
    // interno de sistema (fuera de un request HTTP autenticado, no hay
    // Authentication en el SecurityContext) — ver RevisionInfoListener.
    @Column(name = "usuario_id")
    private Long usuarioId;

    // Empresa del usuario que hizo el cambio (redundante con la
    // empresa_id que cada _aud ya guarda por fila, pero deja la
    // revision en si misma consultable por empresa sin tener que unir
    // contra una _aud especifica).
    @Column(name = "empresa_id")
    private Long empresaId;
}
