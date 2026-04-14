import { Empresa } from "./empresa";

export interface Bodega {
  id?: number;
  nombre: string;
  ubicacionFisica: string;
  empresa: Empresa;
  activo: boolean;
}