import { Empresa } from "./empresa";

export interface Repuesto {
  id?: number;
  codigo: string;
  nombre: string;
  descripcion: string;
  costoUnitario: number;
  stockMinimo: number;
  empresa: Empresa;
  activo: boolean;
}