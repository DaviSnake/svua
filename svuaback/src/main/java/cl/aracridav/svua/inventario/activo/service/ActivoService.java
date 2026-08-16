package cl.aracridav.svua.inventario.activo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import cl.aracridav.svua.inventario.activo.dto.request.ActivoCreateRequest;
import cl.aracridav.svua.inventario.activo.dto.request.ActivoUpdateRequest;
import cl.aracridav.svua.inventario.activo.dto.request.DarDeBajaActivoRequest;
import cl.aracridav.svua.inventario.activo.dto.response.ActivoEscaneoResponse;
import cl.aracridav.svua.inventario.activo.dto.response.ActivoResponse;
import cl.aracridav.svua.shared.enums.EstadoActivo;

public interface ActivoService {

    public ActivoResponse crearActivo(ActivoCreateRequest nuevoEstado);
    public ActivoResponse actualizarActivo(Long activoId, ActivoUpdateRequest request);
    public Page<ActivoResponse> mostrarActivos(Pageable pageable, Long empresaId);
    public void darDeBaja(Long activoId, DarDeBajaActivoRequest request);
    public void actualizarEstado(Long idActivo, EstadoActivo estado);
    public double calcularRiesgo(Long activoId);
    public String nivelRiesgo(double riesgo);

    // 🔳 Escaneo de QR/EAN13: busca el activo por cualquiera de los dos
    // codigos y devuelve, junto a sus datos, todo su historial de
    // ordenes de mantenimiento.
    public ActivoEscaneoResponse buscarPorCodigoEscaneado(String codigo);

}
