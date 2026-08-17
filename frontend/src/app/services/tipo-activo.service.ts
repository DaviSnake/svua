import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Page } from '../shared/page';
import { TipoActivo } from '../model/tipoActivo';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class TipoActivoService {
  
  private apiUrl = environment.apiUrl;
  private http = inject(HttpClient);

  getAll(page = 0, size = 10, empresaId?: number | null): Observable<Page<TipoActivo>> {
    let url = `${this.apiUrl}/tipos-activo?page=${page}&size=${size}&sort=nombre,asc`;

    // 🔥 Filtro por empresa (solo tiene efecto real para SUPER_ADMIN; el
    // backend lo ignora para el resto de los roles).
    if (empresaId) {
      url += `&empresaId=${empresaId}`;
    }

    return this.http.get<Page<TipoActivo>>(url);
  }

  getTipoActivoCombo(page = 0, size = 50, empresaId?: number | null): Observable<Page<TipoActivo>> {
    let url = `${this.apiUrl}/tipos-activo?page=${page}&size=${size}&sort=nombre,asc`;

    // 🔥 Filtro por empresa (solo tiene efecto real para SUPER_ADMIN; el
    // backend lo ignora para el resto de los roles).
    if (empresaId) {
      url += `&empresaId=${empresaId}`;
    }

    return this.http.get<Page<TipoActivo>>(url);
  }

  create(tipoActivo: TipoActivo): Observable<TipoActivo> {
    return this.http.post<TipoActivo>(`${this.apiUrl}/tipos-activo`, tipoActivo);
  }

  update(id: number, tipoActivo: TipoActivo): Observable<TipoActivo> {
    return this.http.put<TipoActivo>(`${this.apiUrl}/tipos-activo/${id}`, tipoActivo);
  }
  
  delete(id: number): Observable<TipoActivo> {
    return this.http.delete<TipoActivo>(`${this.apiUrl}/tipos-activo/${id}`);
  }
}
