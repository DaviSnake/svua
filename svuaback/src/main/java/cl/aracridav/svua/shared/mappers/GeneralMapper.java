package cl.aracridav.svua.shared.mappers;

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
import cl.aracridav.svua.mantenimiento.orden.dto.response.OrdenMantenimientoResponse;
import cl.aracridav.svua.mantenimiento.orden.entity.OrdenMantenimiento;
import cl.aracridav.svua.mantenimiento.ordenrepuesto.dto.response.OrdenRepuestoResponse;
import cl.aracridav.svua.mantenimiento.ordenrepuesto.entity.OrdenRepuesto;
import cl.aracridav.svua.mantenimiento.plan.dto.response.PlanMantenimientoReponse;
import cl.aracridav.svua.mantenimiento.plan.entity.PlanMantenimiento;
import cl.aracridav.svua.mantenimiento.repuesto.dto.response.RepuestoResponse;
import cl.aracridav.svua.mantenimiento.repuesto.entity.Repuesto;
import cl.aracridav.svua.proveedor.dto.response.ProveedorResponse;
import cl.aracridav.svua.proveedor.entity.Proveedor;
import cl.aracridav.svua.shared.dto.response.EmpresaDTO;
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
      .build();
  }

  public TipoActivoResponse mapTipoActivoResponse (TipoActivo tipoActivo){
      
    return TipoActivoResponse.builder()
      .id(tipoActivo.getId())
      .nombre(tipoActivo.getNombre())
      .descripcion(tipoActivo.getDescripcion())
      .vidaUtilReferencialMeses(tipoActivo.getVidaUtilReferencialMeses())
      .empresa(tipoActivo.getEmpresa())
      .build();
  }

  public UbicacionResponse mapUbicacionResponse (Ubicacion ubicacion){
      
    return UbicacionResponse.builder()
      .id(ubicacion.getId())
      .nombre(ubicacion.getNombre())
      .descripcion(ubicacion.getDescripcion())
      .direccion(ubicacion.getDireccion())
      .empresa(ubicacion.getEmpresa())
      .build();
  }

  public ProveedorResponse mapProeedorResponse (Proveedor proveedor){
      
    return ProveedorResponse.builder()
      .id(proveedor.getId())
      .nombre(proveedor.getNombre())
      .rut(proveedor.getRut())
      .contacto(proveedor.getContacto())
      .telefono(proveedor.getTelefono())
      .email(proveedor.getEmail())
      .empresa(proveedor.getEmpresa())
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
      .costoUnitario(repuesto.getCostoUnitario())
      .stockMinimo(repuesto.getStockMinimo())
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
    oMantenimientoResponse.setFechaEjecucion(oMantenimiento.getFechaEjecucion());
    oMantenimientoResponse.setTipoMantenimiento(oMantenimiento.getTipoMantenimiento());
    oMantenimientoResponse.setEstado(oMantenimiento.getEstado());
    oMantenimientoResponse.setCosto(oMantenimiento.getCosto());
    oMantenimientoResponse.setObservaciones(oMantenimiento.getObservaciones());
    oMantenimientoResponse.setActivoId(oMantenimiento.getActivo().getId());
    oMantenimientoResponse.setUsuarioId(oMantenimiento.getUsuario().getId());
          

    return oMantenimientoResponse;
  }

  public OrdenRepuestoResponse mapOrdenRepuestoResponse(OrdenRepuesto oRepuesto) {

    OrdenRepuestoResponse ordenRepuestoResponse = new OrdenRepuestoResponse();

    ordenRepuestoResponse.setId(oRepuesto.getId());
    ordenRepuestoResponse.setOrdenId(oRepuesto.getOrden().getId());
    ordenRepuestoResponse.setRepuestoId(oRepuesto.getRepuesto().getId());
    ordenRepuestoResponse.setCantidad(oRepuesto.getCantidad());
    ordenRepuestoResponse.setCostoUnitario(oRepuesto.getCostoUnitario());
    ordenRepuestoResponse.setCostoTotal(oRepuesto.getCostoTotal());             

    return ordenRepuestoResponse;
  }

  public PlanMantenimientoReponse mapPlanMantenimientotoResponse(PlanMantenimiento entity) {

    PlanMantenimientoReponse dto = new PlanMantenimientoReponse();

    dto.setId(entity.getId());
    dto.setTipoMantenimiento(entity.getTipoMantenimiento());
    dto.setFrecuenciaDias(entity.getFrecuenciaDias());
    dto.setDescripcion(entity.getDescripcion());
    dto.setEstaActivo(entity.getEstaActivo());
    dto.setUltimaEjecucion(entity.getUltimaEjecucion());
    dto.setProximaEjecucion(entity.getProximaEjecucion());

    if (entity.getActivo() != null) {
        dto.setActivoId(entity.getActivo().getId());
    }

    return dto;
  }

}
