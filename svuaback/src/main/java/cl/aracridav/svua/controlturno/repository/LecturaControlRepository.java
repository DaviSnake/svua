package cl.aracridav.svua.controlturno.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import cl.aracridav.svua.controlturno.entity.LecturaControl;

// 🔥 Los filtros dinamicos (puntoControlId/desde/hasta/turno, todos
// opcionales) se arman con Specification (ver LecturaControlSpecs),
// NO con el patron JPQL "(:param IS NULL OR ...)" que se uso en un
// principio: ese patron rompe en Postgres para parametros LocalDateTime
// que llegan en null (ERROR 42P18 "could not determine data type of
// parameter $N" -- el parametro solo aparece en un "? IS NULL" y el
// driver no tiene forma de inferirle un tipo concreto). Specification
// arma el WHERE sin ese predicado cuando el filtro es null, evitando
// el problema de raiz en vez de parchar el sintoma.
public interface LecturaControlRepository
        extends JpaRepository<LecturaControl, Long>, JpaSpecificationExecutor<LecturaControl> {
}
