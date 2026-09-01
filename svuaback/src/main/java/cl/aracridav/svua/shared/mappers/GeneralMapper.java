package cl.aracridav.svua.shared.mappers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;

import org.springframework.stereotype.Component;

import cl.aracridav.svua.empresa.dto.response.EmpresaResponse;
import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.inventario.activo.dto.response.ActivoResponse;
import cl.aracridav.svua.inventario.activo.dto.response.ProveedorDTO;
import cl.aracridav.svua.inventario.activo.dto.response.TipoActivoDTO;
import cl.aracridav.svua.inventario.activo.dto.response.UbicacionDTO;
import cl.aracridav.svua.inventario.activo.entity.Activo;
import cl.aracridav.svua.inventario.bodega.dto.response.BodegaResponse;
import cl.aracridav.svua.inventario.bodega.entity.Bodega;
import cl.aracridav.svua.inventario.historial.dto.response.HistorialEstadoActivoResponse;
import cl.aracridav.svua.inventario.historial.entity.HistorialEstadoActivo;
import cl.aracridav.svua.inventario.movimientoinventario.dto.response.MovimientoInventarioResponse;
import cl.aracridav.svua.inventario.movimientoinventario.entity.MovimientoInventario;
import cl.aracridav.svua.inventario.tipoactivo.dto.response.TipoActivoResponse;
import cl.aracridav.svua.inventario.tipoactivo.entity.TipoActivo;
import cl.aracridav.svua.inventario.ubicacion.dto.response.UbicacionResponse;
import cl.aracridav.svua.inventario.ubicacion.entity.Ubicacion;
import cl.aracridav.svua.mantenimiento.orden.dto.response.OrdenEjecucionResponse;
import cl.aracridav.svua.mantenimiento.orden.dto.response.OrdenMantenimientoResponse;
import cl.aracridav.svua.mantenimiento.orden.entity.OrdenMantenimiento;
import cl.aracridav.svua.mantenimiento.ordenrepuesto.dto.response.OrdenRepuestoResponse;
import cl.aracridav.svua.mantenimiento.ordenrepuesto.entity.OrdenRepuesto;
import cl.aracridav.svua.mantenimiento.repuesto.dto.response.RepuestoResponse;
import cl.aracridav.svua.mantenimiento.repuesto.entity.Repuesto;
import cl.aracridav.svua.notificacion.dto.response.NotificacionResponse;
import cl.aracridav.svua.notificacion.entity.Notificacion;
import cl.aracridav.svua.proveedor.dto.response.ProveedorResponse;
import cl.aracridav.svua.proveedor.entity.Proveedor;
import cl.aracridav.svua.shared.dto.response.EmpresaDTO;
import cl.aracridav.svua.usuario.dto.response.PerfilUsuarioDTO;
import cl.aracridav.svua.usuario.dto.response.UsuarioResponse;
import cl.aracridav.svua.usuario.entity.Usuario;
import lombok.*;

@Component
@Builder
@Data
public class GeneralMapper {

  public UsuarioResponse mapUsuarioToResponse(Usuario usuario) {

    return UsuarioResponse.builder()
      .id(usuario.getId())
      .nombre(usuario.getNombre())
      .rol(usuario.getRol())
      .email(usuario.getEmail())
      .activo(usuario.getActivo())
      .empresaId(usuario.getEmpresa().getId())
      .empresaNombre(usuario.getEmpresa().getNombre())
      .build();
  }

  public EmpresaResponse mapEmpresaToResponse(Empresa empresa) {

    return EmpresaResponse.builder()
      .id(empresa.getId())
      .nombre(empresa.getNombre())
      .rut(empresa.getRut())
      .emailContacto(empresa.getEmailContacto())
      .telefono(empresa.getTelefono())
      .direccion(empresa.getDireccion())
      .activa(empresa.getActiva())
      .tipoPlan(empresa.getTipoPlan())
      .fechaInicioPlan(empresa.getFechaInicioPlan())
      .fechaFinPlan(empresa.getFechaFinPlan())
      .maxUsuarios(empresa.getMaxUsuarios())
      .maxActivos(empresa.getMaxActivos())
      .fechaCreacion(empresa.getFechaCreacion())
      .fechaActualizacion(empresa.getFechaActualizacion())
      .demo(empresa.getDemo())
      .codigoQrHabilitado(empresa.getCodigoQrHabilitado())
      .codigoEan13Habilitado(empresa.getCodigoEan13Habilitado())
      .controlTurnoHabilitado(empresa.getControlTurnoHabilitado())
      .hojaControlHabilitado(empresa.getHojaControlHabilitado())
      .informeMantencionesHabilitado(empresa.getInformeMantencionesHabilitado())
      .build();
  }

  public TipoActivoResponse mapTipoActivoResponse (TipoActivo tipoActivo){

    EmpresaDTO empresaDTO = EmpresaDTO.builder()
      .id(tipoActivo.getEmpresa().getId())
      .rut(tipoActivo.getEmpresa().getRut())
      .nombre(tipoActivo.getEmpresa().getNombre())
      .telefono(tipoActivo.getEmpresa().getTelefono())
      .emailContacto(tipoActivo.getEmpresa().getEmailContacto())
      .tipoPlan(tipoActivo.getEmpresa().getTipoPlan())
      .build();
      
    return TipoActivoResponse.builder()
      .id(tipoActivo.getId())
      .nombre(tipoActivo.getNombre())
      .descripcion(tipoActivo.getDescripcion())
      .vidaUtilReferencialMeses(tipoActivo.getVidaUtilReferencialMeses())
      .activo(tipoActivo.getActivo())
      .empresa(empresaDTO)
      .build();
  }

  public UbicacionResponse mapUbicacionResponse (Ubicacion ubicacion){

    EmpresaDTO empresaDTO = EmpresaDTO.builder()
      .id(ubicacion.getEmpresa().getId())
      .rut(ubicacion.getEmpresa().getRut())
      .nombre(ubicacion.getEmpresa().getNombre())
      .telefono(ubicacion.getEmpresa().getTelefono())
      .emailContacto(ubicacion.getEmpresa().getEmailContacto())
      .tipoPlan(ubicacion.getEmpresa().getTipoPlan())
      .build();
      
    return UbicacionResponse.builder()
      .id(ubicacion.getId())
      .nombre(ubicacion.getNombre())
      .descripcion(ubicacion.getDescripcion())
      .direccion(ubicacion.getDireccion())
      .activo(ubicacion.getActivo())
      .empresa(empresaDTO)
      .build();
  }

  public ProveedorResponse mapProeedorResponse (Proveedor proveedor){

    EmpresaDTO empresaDTO = EmpresaDTO.builder()
      .id(proveedor.getEmpresa().getId())
      .rut(proveedor.getEmpresa().getRut())
      .nombre(proveedor.getEmpresa().getNombre())
      .telefono(proveedor.getEmpresa().getTelefono())
      .emailContacto(proveedor.getEmpresa().getEmailContacto())
      .tipoPlan(proveedor.getEmpresa().getTipoPlan())
      .build();
      
    return ProveedorResponse.builder()
      .id(proveedor.getId())
      .nombre(proveedor.getNombre())
      .rut(proveedor.getRut())
      .contacto(proveedor.getContacto())
      .telefono(proveedor.getTelefono())
      .email(proveedor.getEmail())
      .activo(proveedor.getActivo())
      .tipoProveedor(proveedor.getTipoProveedor())
      .empresa(empresaDTO)
      .build();
  }

  public ActivoResponse mapActivoResponse (Activo activo){
      
    TipoActivoDTO tipoActivoDTO = TipoActivoDTO.builder()
      .id(activo.getTipoActivo().getId())
      .nombre(activo.getTipoActivo().getNombre())
      .vidaUtilReferencialMeses(activo.getTipoActivo().getVidaUtilReferencialMeses())
      .build();

    UbicacionDTO ubicacionDTO = UbicacionDTO.builder()
      .id(activo.getUbicacion().getId())
      .nombre(activo.getUbicacion().getNombre())
      .build();
    
    ProveedorDTO proveedorDTO = ProveedorDTO.builder()
      .id(activo.getProveedor().getId())
      .nombre(activo.getProveedor().getNombre())
      .contacto(activo.getProveedor().getContacto())
      .build();

    EmpresaDTO empresaDTO = EmpresaDTO.builder()
      .id(activo.getEmpresa().getId())
      .rut(activo.getEmpresa().getRut())
      .nombre(activo.getEmpresa().getNombre())
      .telefono(activo.getEmpresa().getTelefono())
      .emailContacto(activo.getEmpresa().getEmailContacto())
      .tipoPlan(activo.getEmpresa().getTipoPlan())
      .build();

    return ActivoResponse.builder()
      .id(activo.getId())
      .codigoInterno(activo.getCodigoInterno())
      .nombre(activo.getNombre())
      .descripcion(activo.getDescripcion())
      .marca(activo.getMarca())
      .modelo(activo.getModelo())
      .numeroSerie(activo.getNumeroSerie())
      .fechaAdquisicion(activo.getFechaAdquisicion())
      .valorAdquisicion(activo.getValorAdquisicion())
      .valorResidual(activo.getValorResidual())
      .vidaUtilMeses(activo.getVidaUtilMeses())
      .estadoActual(activo.getEstadoActual())
      .cuentaContable(activo.getCuentaContable())
      .codigoQr(activo.getCodigoQr())
      .codigoEan13(activo.getCodigoEan13())
      .tipoActivo(tipoActivoDTO)
      .ubicacion(ubicacionDTO)
      .proveedor(proveedorDTO)
      .empresa(empresaDTO)
      .build();
  }

  public BodegaResponse mapBodegaResponse (Bodega bodega){

    EmpresaDTO empresaDTO = EmpresaDTO.builder()
      .id(bodega.getEmpresa().getId())
      .rut(bodega.getEmpresa().getRut())
      .nombre(bodega.getEmpresa().getNombre())
      .telefono(bodega.getEmpresa().getTelefono())
      .emailContacto(bodega.getEmpresa().getEmailContacto())
      .tipoPlan(bodega.getEmpresa().getTipoPlan())
      .build();
    
    return BodegaResponse.builder()
      .id(bodega.getId())
      .nombre(bodega.getNombre())
      .ubicacionFisica(bodega.getUbicacionFisica())
      .activo(bodega.getActiva())
      .empresa(empresaDTO)
      .build();
  }

  public MovimientoInventarioResponse mapMovimientoInventarioResponse (MovimientoInventario mInventario){
      
    return MovimientoInventarioResponse.builder()
      .id(mInventario.getId())
      .repuestoId(mInventario.getRepuesto().getId())
      .tipo(mInventario.getTipo().toString())
      .cantidad(mInventario.getCantidad())
      .fecha(mInventario.getFecha())
      .referencia(mInventario.getReferencia())
      .build();
  }

  public RepuestoResponse mapRepuestoResponse(Repuesto repuesto) {
    
    EmpresaDTO empresaDTO = EmpresaDTO.builder()
      .id(repuesto.getEmpresa().getId())
      .rut(repuesto.getEmpresa().getRut())
      .nombre(repuesto.getEmpresa().getNombre())
      .telefono(repuesto.getEmpresa().getTelefono())
      .emailContacto(repuesto.getEmpresa().getEmailContacto())
      .tipoPlan(repuesto.getEmpresa().getTipoPlan())
      .build();

    return RepuestoResponse.builder()
      .id(repuesto.getId())
      .codigo(repuesto.getCodigo())
      .nombre(repuesto.getNombre())
      .descripcion(repuesto.getDescripcion())
      .cuentaContable(repuesto.getCuentaContable())
      .costoUnitario(repuesto.getCostoUnitario())
      .stockActual(repuesto.getStockActual())
      .stockMinimo(repuesto.getStockMinimo())
      .tipoRepuesto(repuesto.getTipo())
      .activo(repuesto.getActivo())
      .empresa(empresaDTO)
      .build();

  }

  public HistorialEstadoActivoResponse mapHistorialEstadoActivoResponse(HistorialEstadoActivo hEstadoActivo) {

    HistorialEstadoActivoResponse hEstadoActivoResponse = new HistorialEstadoActivoResponse();

    hEstadoActivoResponse.setId(hEstadoActivo.getId());
    hEstadoActivoResponse.setEstado(hEstadoActivo.getEstado());
    hEstadoActivoResponse.setFecha(hEstadoActivo.getFecha());
    hEstadoActivoResponse.setComentario(hEstadoActivo.getComentario());       

    return hEstadoActivoResponse;
  }

  public OrdenMantenimientoResponse mapOrdenMantenimientoResponse(OrdenMantenimiento oMantenimiento) {

    OrdenMantenimientoResponse oMantenimientoResponse = new OrdenMantenimientoResponse();

    oMantenimientoResponse.setTitulo(oMantenimiento.getTitulo());
    oMantenimientoResponse.setId(oMantenimiento.getId());
    oMantenimientoResponse.setFechaProgramada(oMantenimiento.getFechaProgramada());
    oMantenimientoResponse.setFechaTermino(oMantenimiento.getFechaTermino());
    oMantenimientoResponse.setDuracionMinutos(Duration
        .between(oMantenimiento.getFechaProgramada(), oMantenimiento.getFechaTermino())
        .toMinutes());
    oMantenimientoResponse.setDuracionEstimadaSegundos(oMantenimiento.getDuracionEstimadaSegundos());
    oMantenimientoResponse.setFechaEjecucion(oMantenimiento.getFechaEjecucion());
    oMantenimientoResponse.setFechaFinEjecucion(oMantenimiento.getFechaFinEjecucion());
    oMantenimientoResponse.setTipoMantenimiento(oMantenimiento.getTipoMantenimiento());
    oMantenimientoResponse.setEstado(oMantenimiento.getEstado());
    oMantenimientoResponse.setCostoTotal(oMantenimiento.getCostoTotal());

    // 🔥 la hora estimada se calcula siempre desde duracionEstimadaSegundos
    // (la duracion planificada, registrada una sola vez al crear la
    // orden y nunca modificada despues) en vez del campo persistido
    // horasEstimadasProveedor, que en ordenes creadas antes de este
    // cambio puede haber quedado desactualizado. Fallback al campo
    // persistido solo si por algun motivo no hay duracion estimada.
    BigDecimal horasEstimadas = oMantenimiento.getDuracionEstimadaSegundos() != null
        ? BigDecimal.valueOf(oMantenimiento.getDuracionEstimadaSegundos())
            .divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP)
        : oMantenimiento.getHorasEstimadasProveedor();

    oMantenimientoResponse.setHorasEstimadas(horasEstimadas);
    oMantenimientoResponse.setHorasReal(oMantenimiento.getHorasRealesProveedor());
    oMantenimientoResponse.setValorHora(oMantenimiento.getValorHoraProveedor());
    oMantenimientoResponse.setCostoManoObraEstimada(oMantenimiento.getCostoManoObraEstimadasProveedor());
    oMantenimientoResponse.setCostoManoObra(oMantenimiento.getCostoManoObraProveedor());
    oMantenimientoResponse.setObservaciones(oMantenimiento.getObservaciones());
    oMantenimientoResponse.setActivoId(oMantenimiento.getActivo().getId());
    oMantenimientoResponse.setUsuarioId(oMantenimiento.getUsuario().getId());
    oMantenimientoResponse.setProveedorId(oMantenimiento.getProveedor().getId());

    // 🔥 le indica al frontend si ya se adjuntó el checklist (sin
    // exponer la ruta real del archivo en el servidor).
    oMantenimientoResponse.setTieneChecklist(
        oMantenimiento.getRutaArchivo() != null && !oMantenimiento.getRutaArchivo().isBlank()
    );

    if (oMantenimiento.getRepuestosUtilizados() != null){
      // 🔥 REPUESTOS
      List<OrdenRepuestoResponse> repuestos = oMantenimiento.getRepuestosUtilizados()
          .stream()
          .map(this::mapRepuesto)
          .toList();

        oMantenimientoResponse.setRepuestos(repuestos);          
    }


    return oMantenimientoResponse;
  }

  private OrdenRepuestoResponse mapRepuesto(OrdenRepuesto r) {

    OrdenRepuestoResponse dto = new OrdenRepuestoResponse();

    dto.setId(r.getId());

    dto.setRepuestoId(r.getRepuesto().getId());
    dto.setRepuestoNombre(r.getRepuesto().getNombre());

    dto.setCantidad(r.getCantidad());

    dto.setCostoUnitario(r.getCostoUnitario());
    dto.setCostoTotal(r.getCostoTotal());

    return dto;
}

  public OrdenRepuestoResponse mapOrdenRepuestoResponse(OrdenRepuesto oRepuesto) {

    OrdenRepuestoResponse ordenRepuestoResponse = new OrdenRepuestoResponse();

    ordenRepuestoResponse.setId(oRepuesto.getId());
    ordenRepuestoResponse.setOrdenId(oRepuesto.getOrden().getId());
    ordenRepuestoResponse.setRepuestoId(oRepuesto.getRepuesto().getId());
    // 🔥 antes no se seteaba: el frontend siempre recibía el nombre vacío.
    ordenRepuestoResponse.setRepuestoNombre(oRepuesto.getRepuesto().getNombre());
    ordenRepuestoResponse.setCantidad(oRepuesto.getCantidad());
    ordenRepuestoResponse.setCostoUnitario(oRepuesto.getCostoUnitario());
    ordenRepuestoResponse.setCostoTotal(oRepuesto.getCostoTotal());             

    return ordenRepuestoResponse;
  }

  public PerfilUsuarioDTO mapUsuariotoPerfilDTO(Usuario u) {
      PerfilUsuarioDTO dto = new PerfilUsuarioDTO();

      dto.setId(u.getId());
      dto.setNombre(u.getNombre());
      dto.setEmail(u.getEmail());
      dto.setRol(u.getRol().name());
      dto.setActivo(u.getActivo());

      dto.setEmpresaNombre(u.getEmpresa().getNombre());
      dto.setEmpresaRut(u.getEmpresa().getRut());
      dto.setPlan(u.getEmpresa().getTipoPlan().name());
      dto.setFechaFinPlan(u.getEmpresa().getFechaFinPlan());

      return dto;
  }

  public OrdenEjecucionResponse mapOrdenEjecucionResponse(OrdenMantenimiento o) {

      OrdenEjecucionResponse dto = new OrdenEjecucionResponse();

      dto.setId(o.getId());
      dto.setEstado(o.getEstado().name());
      dto.setFechaEjecucion(o.getFechaEjecucion());
      dto.setFechaFinEjecucion(o.getFechaFinEjecucion());
      dto.setDuracionSegundos(o.getDuracionSegundos());
      dto.setTitulo(o.getTitulo());

      if (o.getActivo() != null) {
          dto.setActivoNombre(o.getActivo().getNombre());
      }

      return dto;
  }

  public NotificacionResponse mapNotificacionResponse(
    Notificacion notificacion) {

    return NotificacionResponse.builder()
      .id(notificacion.getId())
      .titulo(notificacion.getTitulo())
      .mensaje(notificacion.getMensaje())
      .leida(notificacion.getLeida())
      .referenciaId(notificacion.getReferenciaId())
      .tipoReferencia(
        notificacion.getTipoReferencia() != null
                ? notificacion.getTipoReferencia().name()
                : null)
      .tipoNotificacion(
        notificacion.getTipoNotificacion().name())
      .fechaCreacion(
        notificacion.getFechaCreacion())
      .build();
    }

}
