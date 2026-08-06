package cl.aracridav.svua.inventario.movimientoinventario.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.aracridav.svua.inventario.movimientoinventario.entity.MovimientoInventario;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

    List<MovimientoInventario> findByEmpresaId(Long empresaId);
}
