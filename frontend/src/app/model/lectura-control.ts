export type TurnoTrabajo = 'MANANA' | 'TARDE' | 'NOCHE';

export interface LecturaControl {
  id?: number;
  puntoControlId: number;
  puntoControlNombre?: string;
  unidad?: string;
  valor: number;
  fechaHora?: string;
  turno: TurnoTrabajo;
  observacion?: string;
  usuarioNombre?: string;
}

// 🔥 Datos pre-agregados que entrega el backend para armar los graficos
// de un punto de control (ver PuntoControlDashboardResponse en el
// backend): el frontend solo dibuja, no recalcula nada.
export interface PuntoControlDashboard {
  puntoControlId: number;
  nombre: string;
  unidad: string;
  valorMin?: number | null;
  valorMax?: number | null;
  fechas: string[];
  valores: number[];
  lecturasDentroRango: number;
  lecturasFueraRango: number;
}
