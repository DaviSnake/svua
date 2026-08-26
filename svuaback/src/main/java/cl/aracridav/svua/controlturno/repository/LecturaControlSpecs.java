package cl.aracridav.svua.controlturno.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

import cl.aracridav.svua.controlturno.entity.LecturaControl;
import cl.aracridav.svua.controlturno.enums.TurnoTrabajo;

// 🔥 Predicados dinamicos para el listado/dashboard de lecturas (ver
// LecturaControlServiceImpl). Cada metodo devuelve null cuando el
// filtro no aplica; Specification.where(...).and(...) ignora los specs
// null automaticamente, asi el predicado ni siquiera se arma en el SQL
// final cuando el filtro viene vacio (ver LecturaControlRepository
// para el motivo: evitar el bug de tipos de Postgres con parametros
// LocalDateTime null en un "? IS NULL").
public final class LecturaControlSpecs {

    private LecturaControlSpecs() {
    }

    public static Specification<LecturaControl> empresaId(Long empresaId) {
        if (empresaId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("empresa").get("id"), empresaId);
    }

    public static Specification<LecturaControl> puntoControlId(Long puntoControlId) {
        if (puntoControlId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("puntoControl").get("id"), puntoControlId);
    }

    public static Specification<LecturaControl> desde(LocalDateTime desde) {
        if (desde == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("fechaHora"), desde);
    }

    public static Specification<LecturaControl> hasta(LocalDateTime hasta) {
        if (hasta == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("fechaHora"), hasta);
    }

    public static Specification<LecturaControl> turno(TurnoTrabajo turno) {
        if (turno == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("turno"), turno);
    }
}
