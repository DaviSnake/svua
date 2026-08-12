import { HistorialActivoEvento } from "./historial-eventos.model";

export interface HistorialActivoCompleto {
  activoId: number;
  nombreActivo: string;
  valorAdquisicion: number;
  valorResidual: number;
  costoMantenciones: number;
  cantidadMantenciones: number;
  empresaId?: number;
  empresaNombre?: string;
  eventos: HistorialActivoEvento[];
}