import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Page } from '../shared/page';
import { Ubicacion } from '../model/ubicacion';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class UbicacionService {

  private apiUrl = environment.apiUrl;
  private http = inject(HttpClient);

  getAll(page = 0, size = 10, empresaId?: number | null): Observable<Page<Ubicacion>> {
    let url = `${this.apiUrl}/ubicaciones?page=${page}&size=${size}&sort=nombre,asc`;

    // 🔥 Filtro por empresa (solo tiene efecto real para SUPER_ADMIN; el
    // backend lo ignora para el resto de los roles).
    if (empresaId) {
      url += `&empresaId=${empresaId}`;
    }

    return this.http.get<Page<Ubicacion>>(url);
  }

  getUbicacionCombo(page = 0, size = 50, empresaId?: number | null): Observable<Page<Ubicacion>> {
    let url = `${this.apiUrl}/ubicaciones?page=${page}&size=${size}&sort=nombre,asc`;

    // 🔥 Filtro por empresa (solo tiene efecto real para SUPER_ADMIN; el
    // backend lo ignora para el resto de los roles).
    if (empresaId) {
      url += `&empresaId=${empresaId}`;
    }

    return this.http.get<Page<Ubicacion>>(url);
  }

  create(ubicacion: Ubicacion): Observable<Ubicacion> {
    return this.http.post<Ubicacion>(`${this.apiUrl}/ubicaciones`, ubicacion);
  }

  update(id: number, ubicacion: Ubicacion): Observable<Ubicacion> {
    return this.http.put<Ubicacion>(`${this.apiUrl}/ubicaciones/${id}`, ubicacion);
  }
  
  delete(id: number): Observable<Ubicacion> {
    return this.http.delete<Ubicacion>(`${this.apiUrl}/ubicaciones/${id}`);
  }
}
