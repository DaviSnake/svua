package cl.aracridav.svua.inventario.bodega.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.inventario.bodega.entity.Bodega;

public interface BodegaRepository extends JpaRepository<Bodega, Long> {

    Page<Bodega> findByEmpresaId(Long empresaId, Pageable pageable);

    List<Bodega> findByEmpresaAndActivaTrue(Empresa empresa);

    boolean existsByNombreIgnoreCaseAndEmpresaId(String nombre, Long empresaId);

}
