export interface HistorialActivoEvento {
  fecha: string;
  fechaProgramada: string;
  fechaEjecucion: string;
  tipo: string;
  tipoMantenimiento: string | null;
  descripcion: string;
  usuario: string | null;
  proveedor: string | null;
  costoTotal: number | null;
  valorHora: number | null;
  costoManoObra: number | null;
  horasTrabajo: number | null;
  repuestos: string[] | null;
}