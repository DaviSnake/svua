package cl.aracridav.svua.mantenimiento.orden.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import cl.aracridav.svua.mantenimiento.orden.dto.request.ActualizarOrdenMantenimientoRequest;
import cl.aracridav.svua.mantenimiento.orden.dto.request.OrdenMantenimientoRequest;
import cl.aracridav.svua.mantenimiento.orden.dto.response.CostosGraficoReponse;
import cl.aracridav.svua.mantenimiento.orden.dto.response.OrdenEjecucionResponse;
import cl.aracridav.svua.mantenimiento.orden.dto.response.OrdenMantenimientoReporteResponse;
import cl.aracridav.svua.mantenimiento.orden.dto.response.OrdenMantenimientoResponse;
import cl.aracridav.svua.mantenimiento.orden.entity.EstadoOrden;
import cl.aracridav.svua.mantenimiento.orden.entity.OrdenMantenimiento;

public interface OrdenMantenimientoService {

    public OrdenMantenimientoResponse crearOrden(OrdenMantenimientoRequest request);

    public OrdenEjecucionResponse cerrarOrden(
            Long ordenId,
            BigDecimal costo,
            String observacionesFinales
    );

    public OrdenEjecucionResponse detenerOrden(Long idOrden);

    public OrdenEjecucionResponse preDetenerOrden(Long idOrden, MultipartFile archivo);

    // 🔥 permite adjuntar (o reemplazar) el checklist DESPUÉS de que la
    // orden ya quedó PRE_COMPLETADA sin él — cubre el plazo de 24h que
    // se le avisa al usuario en el frontend.
    public OrdenEjecucionResponse subirChecklist(Long idOrden, MultipartFile archivo);

    public void cancelarOrden(Long id, String motivo, Long usuarioId);

    public List<OrdenMantenimiento> obtenerOrdenesVencidas();

    public OrdenEjecucionResponse ejecutarOrden(Long idOrden);

    public List<OrdenMantenimientoResponse> listarOrdenesEmpresa(Long empresaIdFiltro);
    
    public OrdenMantenimientoResponse reprogramarOrden(Long ordenId, LocalDateTime nuevaFecha, String motivo);

    public OrdenMantenimientoResponse actualizar(
            Long id,
            ActualizarOrdenMantenimientoRequest request);

    // 🔥 activoId es opcional: filtra el grafico de evolucion de
    // costos a un solo activo (disponible para todos los usuarios).
    // 🔒 empresaId es opcional y solo tiene efecto para SUPER_ADMIN.
    public CostosGraficoReponse obtenerGraficoCostosUltimos6Meses(
            Long activoId, Long empresaId);

    public Resource obtenerArchivo(Long id);

    // 🔥 Informe de Mantenciones: historial paginado y filtrable de
    // ordenes COMPLETADAS, visible solo para SUPER_ADMIN.
    public Page<OrdenMantenimientoReporteResponse> obtenerInformeMantenciones(
            String usuario,
            Long empresaId,
            EstadoOrden estado,
            LocalDate fecha,
            Pageable pageable);
}
