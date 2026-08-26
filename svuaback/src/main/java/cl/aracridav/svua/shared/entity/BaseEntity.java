package cl.aracridav.svua.shared.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import cl.aracridav.svua.empresa.entity.Empresa;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
@FilterDef(name = "empresaFilter", parameters = @ParamDef(name = "empresaId", type = Long.class))
@Filter(name = "empresaFilter", condition = "empresa_id = :empresaId")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false, updatable = false)
    private Empresa empresa;

    // 🔥 fecha de creacion centralizada para TODAS las entidades que
    // extienden BaseEntity: Hibernate/Spring la completa solo (via
    // AuditingEntityListener, ver @EnableJpaAuditing en
    // SvuabackApplication) al hacer el insert, sin tocar el codigo de
    // cada servicio. updatable=false evita que un UPDATE la pise.
    //
    // ⚠️ Activo y Notificacion NO heredan este campo con valor propio:
    // ya tenian su propio "fecha_creacion" declarado en su entidad antes
    // de este cambio, asi que se les quito esa declaracion duplicada
    // para que usen esta unica columna heredada (sus datos historicos
    // en la tabla no se tocan, la migracion solo agrega la columna en
    // las tablas que todavia no la tenian).
    @CreatedDate
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    // 🔥 quien creo la fila (ver UsuarioAuditorAware, que lee el usuario
    // autenticado del SecurityContext). Fuera de un request HTTP
    // autenticado (schedulers, jobs internos) queda en NULL: se
    // interpreta como "sistema", no como un dato faltante por error.
    @CreatedBy
    @Column(name = "creado_por_id", updatable = false)
    private Long creadoPorId;

    // 🔥 a diferencia de fechaCreacion (se completa una sola vez), esta
    // se actualiza en CADA update. Junto con creadoPorId/modificadoPorId
    // cierra la brecha de trazabilidad que pedia NCh-ISO/IEC 27001 A.12
    // (Seguridad de operaciones): quien y cuando modifico un registro
    // por ultima vez, no solo quien/cuando lo creo.
    //
    // ⚠️ Esto NO es un log de auditoria completo (no guarda el
    // historico de cada cambio ni los valores anteriores de cada
    // campo) -- solo la ULTIMA modificacion. Un historial completo de
    // cambios (quien cambio que campo, de que valor a que valor, en
    // cada version) es una funcionalidad aparte y mas pesada (ej. con
    // Hibernate Envers); esto no la reemplaza.
    @LastModifiedDate
    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    @LastModifiedBy
    @Column(name = "modificado_por_id")
    private Long modificadoPorId;

}
