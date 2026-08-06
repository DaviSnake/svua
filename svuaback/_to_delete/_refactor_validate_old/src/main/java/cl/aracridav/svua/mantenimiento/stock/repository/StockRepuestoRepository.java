package cl.aracridav.svua.mantenimiento.stock.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.inventario.bodega.entity.Bodega;
import cl.aracridav.svua.mantenimiento.repuesto.entity.Repuesto;
import cl.aracridav.svua.mantenimiento.stock.entity.StockRepuesto;

public interface StockRepuestoRepository extends JpaRepository<StockRepuesto, Long> {

    Optional<StockRepuesto> findByRepuestoAndBodegaAndEmpresa(
            Repuesto repuesto,
            Bodega bodega,
            Empresa empresa
    );

    Optional<StockRepuesto> findByRepuestoIdAndEmpresaId(
            Long repuestoId,
            Long empresaId
    );

}
