package cl.aracridav.svua.shared.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.springframework.data.annotation.CreatedDate;
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

}
