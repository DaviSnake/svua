import { inject, Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { DashboardKPIs } from '../components/dashboard/models/dashboard.model';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { DashboardResponse } from '../components/reportes/models/reportes.model';

@Injectable({
  providedIn: 'root'
})
export class DashboardService {

  private apiUrl = environment.apiUrl;
  http = inject(HttpClient);

  getResumen(): Observable<DashboardKPIs> {
    return of({
      totalActivos: 120,
      activosOperativos: 100,
      activosFueraServicio: 20,
      valorTotal: 50000000,
      depreciacionAcumulada: 12000000,
      ordenesAbiertas: 15,
      mantenimientosVencidos: 5,
      ordenesPorEstado: [5, 7, 3, 10, 5],
      meses: ["Ene","Feb","Mar","Abr","May","Jun"],
      depreciacionMensual: [10000,12000,15000,17000,20000,22000]
    });
  }

  // 🔒 empresaId es opcional y solo tiene efecto si quien llama es
  // SUPER_ADMIN (el backend lo ignora para el resto de los roles).
  getDashboard(empresaId?: number | null): Observable<DashboardKPIs> {
    let url = `${this.apiUrl}/dashboard/full`;

    if (empresaId != null) {
      url += `?empresaId=${empresaId}`;
    }

    return this.http.get<DashboardKPIs>(url);
  }

  // 🔒 empresaId es opcional y solo tiene efecto si quien llama es
  // SUPER_ADMIN (el backend lo ignora para el resto de los roles).
  getDashboardIndicadores(empresaId?: number | null) {
    let url = `${this.apiUrl}/dashboard`;

    if (empresaId != null) {
      url += `?empresaId=${empresaId}`;
    }

    return this.http.get<DashboardResponse>(url);
  }

  // 🔥 activoId es opcional: filtra la evolución de costos de
  // mantención a un solo activo (disponible para todos los usuarios).
  // 🔒 empresaId es opcional y solo tiene efecto para SUPER_ADMIN.
  getCostos(activoId?: number | null, empresaId?: number | null) {
    let url = `${this.apiUrl}/ordenes-mantenimiento/grafico/costos`;

    const params: string[] = [];

    if (activoId != null) {
      params.push(`activoId=${activoId}`);
    }

    if (empresaId != null) {
      params.push(`empresaId=${empresaId}`);
    }

    if (params.length) {
      url += `?${params.join('&')}`;
    }

    return this.http.get<any>(url);
  }

}
