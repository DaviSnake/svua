export interface DashboardResponse {
  programadas: number;
  preCompletadas: number;
  completadas: number;
  pendientes: number;
  atrasadas: number;
  canceladas: number;

  cumplimientoPreventivo: number;
  cumplimientoCorrectivo: number;
  disponibilidad: number;
  mttrHoras: number;
  mtbfHoras: number;
}