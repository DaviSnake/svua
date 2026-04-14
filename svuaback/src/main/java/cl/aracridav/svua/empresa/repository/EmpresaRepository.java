package cl.aracridav.svua.empresa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.aracridav.svua.empresa.entity.Empresa;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    Optional<Empresa> findByRut(String rut);

    boolean existsByRut(String rut);

    boolean existsByNombre(String nombre);

    Optional<Empresa> findById(Long Id);

}
