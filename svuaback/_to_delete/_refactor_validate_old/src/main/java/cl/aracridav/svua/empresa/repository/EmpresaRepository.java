package cl.aracridav.svua.empresa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.aracridav.svua.empresa.entity.Empresa;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    Optional<Empresa> findByRut(String rut);

    boolean existsByRut(String rut);

    boolean existsByNombre(String nombre);

}
