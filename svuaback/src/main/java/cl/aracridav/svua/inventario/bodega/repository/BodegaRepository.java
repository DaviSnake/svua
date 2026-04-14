package cl.aracridav.svua.inventario.bodega.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.inventario.bodega.entity.Bodega;

@Repository
public interface BodegaRepository extends JpaRepository<Bodega, Long> {

    Page<Bodega> findByEmpresaId(Long empresaId, Pageable pegable);

    List<Bodega> findByEmpresaAndActivaTrue(Empresa empresa);

}
