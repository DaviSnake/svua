import { Empresa } from "./empresa";

export interface Proveedor {
  id?: number;
  nombre: string;
  rut: string;
  contacto: string;
  telefono: string;
  email: string;
  tipoProveedor: string;
  empresa: Empresa;
  activo: boolean;
}