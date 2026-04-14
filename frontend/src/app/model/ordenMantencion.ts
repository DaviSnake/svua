export interface OrdenMantencion {
  id?: number;
  titulo?: string;
  observaciones?: string;
  fechaProgramada?: string;
  end?: string;
  estado?: string;
  tipoMantenimiento?: string;
  costo?: string;
  activoId?: string;
  usuarioId?: string;
}