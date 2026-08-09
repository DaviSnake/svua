package cl.aracridav.svua.shared.service;

import java.nio.file.Path;
import java.util.List;

import cl.aracridav.svua.inventario.activo.dto.request.ActivoImportRowDTO;
import cl.aracridav.svua.inventario.activo.dto.response.ActivoImportResultDTO;
import cl.aracridav.svua.inventario.tipoactivo.dto.request.TipoActivoImportRowDTO;
import cl.aracridav.svua.inventario.ubicacion.dto.request.UbicacionImportRowDTO;
import cl.aracridav.svua.mantenimiento.orden.dto.request.OrdenImportRowDTO;
import cl.aracridav.svua.mantenimiento.repuesto.dto.request.RepuestoImportRowDTO;
import cl.aracridav.svua.proveedor.dto.request.ProveedorImportRowDTO;
import cl.aracridav.svua.shared.dto.response.ImportBatchResultDTO;

public interface ExcelImportService {

    public void procesarAsync(Path path, String jobId, Long empresaId, Long usuarioId, String archivo);

    /**
     * Procesa de forma síncrona un lote de Activos ingresados manualmente
     * (grilla tipo planilla), reutilizando las mismas validaciones y reglas
     * de negocio que el importador de Excel.
     */
    public ActivoImportResultDTO procesarActivosManual(List<ActivoImportRowDTO> filas, Long empresaId, Long usuarioId);

    public ImportBatchResultDTO procesarProveedoresManual(List<ProveedorImportRowDTO> filas, Long empresaId);

    public ImportBatchResultDTO procesarOrdenesManual(List<OrdenImportRowDTO> filas, Long empresaId, Long usuarioId);

    public ImportBatchResultDTO procesarRepuestosManual(List<RepuestoImportRowDTO> filas, Long empresaId);

    public ImportBatchResultDTO procesarUbicacionesManual(List<UbicacionImportRowDTO> filas, Long empresaId);

    public ImportBatchResultDTO procesarTiposActivoManual(List<TipoActivoImportRowDTO> filas, Long empresaId);

}
