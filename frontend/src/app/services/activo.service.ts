import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Activo } from '../model/activo';
import { Observable } from 'rxjs';
import { Page } from '../shared/page';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ActivoService {

  private apiUrl = environment.apiUrl;
  private http = inject(HttpClient);

  getAll(page = 0, size = 10, empresaId?: number | null): Observable<Page<Activo>> {
    let url = `${this.apiUrl}/activos?page=${page}&size=${size}&sort=nombre,asc`;

    // 🔥 Filtro por empresa (solo tiene efecto real para SUPER_ADMIN; el
    // backend lo ignora para el resto de los roles).
    if (empresaId) {
      url += `&empresaId=${empresaId}`;
    }

    return this.http.get<Page<Activo>>(url);
  }

  getActivoCombo(page = 0, size = 200): Observable<Page<Activo>> {
    return this.http.get<Page<Activo>>(`${this.apiUrl}/activos?page=${page}&size=${size}&sort=nombre,asc`);
  }

  create(activo: any): Observable<Activo> {
    return this.http.post<Activo>(`${this.apiUrl}/activos`, activo);
  }

  update(id: number, data: Activo) {
    return this.http.put(`${this.apiUrl}/activos/${id}`, data);
  }

  darDeBaja(id: number, motivo: String) {
    const body = {
      motivo: motivo
    };
    return this.http.patch(`${this.apiUrl}/activos/${id}/baja`, body);
  }
}
