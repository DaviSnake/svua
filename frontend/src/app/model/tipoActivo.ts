import { Empresa } from "./empresa";

export interface TipoActivo {
  id?: number;
  nombre: string;
  descripcion: string;
  vidaUtilReferencialMeses: number;
  empresa: Empresa;
  activo: boolean;
}