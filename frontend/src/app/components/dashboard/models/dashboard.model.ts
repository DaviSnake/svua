export interface DashboardKPIs {
  totalActivos: number;
  activosOperativos: number;
  activosFueraServicio: number;
  activosDeBaja: number;
  valorTotal: number;
  depreciacionAcumulada: number;
  ordenesAbiertas: number;
  mantenimientosVencidos: number;

  // PRO
  depreciacionMensual: number[];
  meses: string[];
  ordenesPorEstado: number[];
}