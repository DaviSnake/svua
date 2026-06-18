package cl.aracridav.svua.inventario.tipoactivo.repository;


import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import cl.aracridav.svua.inventario.tipoactivo.entity.TipoActivo;

public interface TipoActivoRepository extends JpaRepository<TipoActivo, Long> {

    Page<TipoActivo> findByEmpresaId(Long empresaId, Pageable pegable);

    Optional<TipoActivo> findFirstByNombre(String nombre);

    boolean existsByNombreIgnoreCaseAndEmpresaId(String nombre, Long empresaId);
    
    Optional<TipoActivo> findFirstByNombreAndEmpresaId(
        String nombre,
        Long empresaId
    );

}
