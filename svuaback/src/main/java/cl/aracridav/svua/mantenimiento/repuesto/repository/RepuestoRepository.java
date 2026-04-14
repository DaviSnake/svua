package cl.aracridav.svua.mantenimiento.repuesto.repository;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.mantenimiento.repuesto.entity.Repuesto;

@Repository
public interface RepuestoRepository extends JpaRepository<Repuesto, Long> {

    Page<Repuesto> findByEmpresa(Empresa empresa, Pageable pegable);

    boolean existsByCodigoAndEmpresa(String codigo, Empresa empresa);

}
