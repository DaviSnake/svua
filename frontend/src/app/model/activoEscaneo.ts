import { Activo } from './activo';
import { OrdenMantencion } from './ordenMantencion';

export interface ActivoEscaneoResponse {
  activo: Activo;
  mantenciones: OrdenMantencion[];
}
