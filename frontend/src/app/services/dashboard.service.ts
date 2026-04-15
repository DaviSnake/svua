import { inject, Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { DashboardKPIs } from '../components/dashboard/models/dashboard.model';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

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

  getDashboard(): Observable<DashboardKPIs> {
  return this.http.get<DashboardKPIs>(`${this.apiUrl}/dashboard/full`);
}

}
