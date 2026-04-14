import { Empresa } from "./empresa";

export interface Ubicacion {
  id?: number;
  nombre: string;
  descripcion: string;
  direccion: string;
  empresa: Empresa;
  activo: boolean;
}