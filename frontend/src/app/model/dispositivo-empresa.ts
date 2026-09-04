export interface DispositivoEmpresa {
  id?: number;
  codigoDispositivo: string;
  descripcion?: string;
  empresaId?: number | null;
  empresaNombre?: string;
  activo?: boolean;
}
