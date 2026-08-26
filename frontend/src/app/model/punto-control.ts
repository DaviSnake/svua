export interface PuntoControl {
  id?: number;
  nombre: string;
  unidad: string;
  valorMin?: number | null;
  valorMax?: number | null;
  activo?: boolean;
  empresaId?: number;
}
