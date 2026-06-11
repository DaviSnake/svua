package cl.aracridav.svua.mantenimiento.repuesto.repository;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.mantenimiento.repuesto.entity.Repuesto;

public interface RepuestoRepository extends JpaRepository<Repuesto, Long> {

    Page<Repuesto> findByEmpresa(Empresa empresa, Pageable pegable);

    @EntityGraph(attributePaths = {"empresa"})
    Page<Repuesto> findByEmpresaId(Long empresaId, Pageable pageable);

    boolean existsByCodigoAndEmpresa(String codigo, Empresa empresa);

}
