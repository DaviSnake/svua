import { inject, Injectable } from '@angular/core';
import { environment } from '../../environments/environment';
import { HttpClient } from '@angular/common/http';
import { HistorialActivoCompleto } from '../components/auditorias/models/historial-completo.model';
import { Observable } from 'rxjs';
import { Page } from '../shared/page';

@Injectable({
  providedIn: 'root'
})
export class HistorialService {

  private apiUrl = environment.apiUrl;
  private http = inject(HttpClient);

  getAll(page = 0, size = 10): Observable<Page<HistorialActivoCompleto>> {
    return this.http.get<Page<HistorialActivoCompleto>>(`${this.apiUrl}/historial-activo/historial?page=${page}&size=${size}&sort=nombre,asc`);
  }

  obtenerHistorialCompleto(empresaId?: number | null): Observable<HistorialActivoCompleto[]> {
    let url = `${this.apiUrl}/historial-activo/historial`;

    // 🔥 Filtro por empresa (solo tiene efecto real para SUPER_ADMIN; el
    // backend lo ignora para el resto de los roles).
    if (empresaId) {
      url += `?empresaId=${empresaId}`;
    }

    return this.http.get<HistorialActivoCompleto[]>(url);
  }
}
