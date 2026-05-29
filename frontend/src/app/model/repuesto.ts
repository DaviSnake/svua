import { Empresa } from "./empresa";

export interface Repuesto {
  id?: number;
  codigo: string;
  nombre: string;
  descripcion: string;
  cuentaContable: string;
  costoUnitario: number;
  stockActual: number;
  stockMinimo: number;
  tipoRepuesto: string;
  empresa: Empresa;
  activo: boolean;
}