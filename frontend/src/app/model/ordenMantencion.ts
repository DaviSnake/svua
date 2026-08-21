import { OrdenRepuesto } from "./ordenRepuesto";

export interface OrdenMantencion {
  id?: number;
  titulo?: string;
  observaciones?: string;
  fechaProgramada?: string;
  fechaTermino?: string;
  fechaEjecucion?: string;
  fechaFinEjecucion?: string;
  duracionMinutos?: string;
  duracionEstimadaSegundos?: number;
  end?: string;
  estado?: string;
  tipoMantenimiento?: string;
  costoTotal?: string;
  activoId?: string;
  usuarioId?: string;
  proveedorId?: string;
  valorHora?: string;
  horasEstimadas?: string;
  horasReal?: string;
  costoManoObraEstimada?: string;
  costoManoObra?: string;
  // 🔥 NUEVO
  repuestos?: OrdenRepuesto[];
  tieneChecklist?: boolean;
}