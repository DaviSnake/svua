import { Observable } from 'rxjs';

export interface ManualGridColumn {
  key: string;
  label: string;
  type?: 'text' | 'number' | 'date' | 'time';
  /** Opciones para autocompletado (datalist) — ej. catálogo de tipos de activo, ubicaciones, proveedores, activos */
  options?: { value: string; label: string }[];
  /**
   * Marca la columna como de solo lectura (valor calculado automáticamente
   * a partir de otras columnas de la misma fila, ver `calcular`). El usuario
   * no puede escribir ni pegar directamente sobre esta celda.
   */
  readonly?: boolean;
  /**
   * Si se define, el valor de esta columna se recalcula automáticamente
   * cada vez que cambia cualquier campo de la fila (tipeo o pegado). Recibe
   * la fila completa (con los valores ya actualizados) y debe devolver el
   * nuevo valor de esta columna.
   */
  calcular?: (fila: any) => any;
}

export interface ManualGridRowResult {
  fila: number;
  exito: boolean;
  mensaje: string;
  referencia?: string;
}

export interface ManualGridBatchResult {
  total: number;
  exitosos: number;
  fallidos: number;
  resultados: ManualGridRowResult[];
}

export type ManualGridImportFn = (filas: any[]) => Observable<ManualGridBatchResult>;
