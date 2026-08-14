import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ConfiguracionEntry } from '../components/configuracion/models/configuracion.model';

// 🔒 Configuración global de la infraestructura (.env real usado por
// docker-compose). Solo SUPER_ADMIN puede llamar a estos endpoints (el
// backend los rechaza para cualquier otro rol).
@Injectable({
  providedIn: 'root'
})
export class ConfiguracionService {

  private apiUrl = environment.apiUrl;
  private http = inject(HttpClient);

  obtenerConfiguracion(): Observable<ConfiguracionEntry[]> {
    return this.http.get<ConfiguracionEntry[]>(`${this.apiUrl}/configuracion`);
  }

  actualizarConfiguracion(valores: Record<string, string>): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/configuracion`, { valores });
  }
}
