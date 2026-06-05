export interface HistorialActivoEvento {
  fecha: string;
  tipo: string;
  descripcion: string;
  usuario: string | null;
  costoTotal: number | null;
  valorHora: number | null;
  costoManoObra: number | null;
  horasTrabajo: number | null;
}