package cl.aracridav.svua.camara.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import cl.aracridav.svua.camara.entity.LecturaTemperatura;

public interface LecturaRepository extends JpaRepository<LecturaTemperatura, Long> {
    List<LecturaTemperatura> findByEmpresaIdOrderByFechaLecturaDesc(Long empresaId);
}
