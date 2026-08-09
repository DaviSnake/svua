export interface ActivoImportRow {
  codigoInterno: string;
  nombre: string;
  descripcion: string;
  tipoActivoNombre: string;
  marca: string;
  modelo: string;
  numeroSerie: string;
  fechaAdquisicion: string; // yyyy-MM-dd (input type="date")
  valorAdquisicion: number | null;
  valorResidual: number | null;
  vidaUtilMeses: number | null;
  ubicacionNombre: string;
  proveedorRut: string;
  cuentaContable: string;
}

export interface ActivoImportRowResult {
  fila: number;
  exito: boolean;
  mensaje: string;
  codigoInterno: string;
  activoId?: number;
}

export interface ActivoImportResult {
  total: number;
  exitosos: number;
  fallidos: number;
  resultados: ActivoImportRowResult[];
}
