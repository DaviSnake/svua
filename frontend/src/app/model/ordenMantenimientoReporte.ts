// 🔥 Informe de Mantenciones: comprobante por orden de mantención
// completada, visible solo para SUPER_ADMIN. Refleja el DTO del backend
// OrdenMantenimientoReporteResponse, que ya resuelve los nombres
// (activo, empresa, usuario, proveedor) para mostrarlos directamente en
// el informe sin llamadas adicionales.
export interface OrdenRepuestoReporte {
  id?: number;
  ordenId?: number;
  repuestoId?: number;
  repuestoNombre?: string;
  cantidad?: number;
  costoUnitario?: number;
  costoTotal?: number;
}

export interface OrdenMantenimientoReporte {
  id?: number;
  titulo?: string;
  observaciones?: string;
  estado?: string;
  tipoMantenimiento?: string;
  fechaProgramada?: string;
  fechaEjecucion?: string;
  duracionSegundos?: number;
  activoNombre?: string;
  empresaNombre?: string;
  usuarioNombre?: string;
  proveedorNombre?: string;
  valorHoraProveedor?: number;
  costoManoObraProveedor?: number;
  costoTotal?: number;
  repuestos?: OrdenRepuestoReporte[];
}
