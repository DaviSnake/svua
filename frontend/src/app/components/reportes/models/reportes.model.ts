export interface DashboardResponse {
  programadas: number;
  preCompletadas: number;
  completadas: number;
  pendientes: number;
  atrasadas: number;
  canceladas: number;

  cumplimiento: number;
  mttrHoras: number;
  mtbfHoras: number;
}