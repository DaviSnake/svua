package cl.aracridav.svua.inventario.activo.dto.response;

import java.util.List;

import cl.aracridav.svua.mantenimiento.orden.dto.response.OrdenMantenimientoResponse;
import lombok.Builder;
import lombok.Data;

/**
 * Respuesta del escaneo de un activo (QR o EAN13): datos del activo mas
 * el listado completo de sus ordenes de mantenimiento, para mostrar todo
 * el historial de mantenciones apenas se lee el codigo.
 */
@Data
@Builder
public class ActivoEscaneoResponse {

    private ActivoResponse activo;
    private List<OrdenMantenimientoResponse> mantenciones;

}
