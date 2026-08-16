import { Empresa } from "./empresa";
import { Proveedor } from "./proveedor";
import { TipoActivo } from "./tipoActivo";
import { Ubicacion } from "./ubicacion";

export interface Activo {
  id?: number;
  codigoInterno?: string;
  codigoQr?: string;
  codigoEan13?: string;
  nombre: string;
  descripcion: string;
  estadoActual: string;
  marca: string;
  tipoActivo: TipoActivo;
  modelo: string;
  numeroSerie: string;
  fechaAdquisicion: string;
  valorResidual: number;
  valorAdquisicion: number;
  vidaUtilMeses: number;
  empresa: Empresa;
  ubicacion: Ubicacion;
  proveedor: Proveedor;
  activo: boolean;
  cuentaContable: string;
}