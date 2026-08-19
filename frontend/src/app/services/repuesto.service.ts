import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Repuesto } from '../model/repuesto';
import { Page } from '../shared/page';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class RepuestoService {

  private apiUrl = environment.apiUrl;
  private http = inject(HttpClient);

  getAll(page = 0, size = 10, empresaId?: number | null, busqueda?: string | null): Observable<Page<Repuesto>> {
    let url = `${this.apiUrl}/repuestos?page=${page}&size=${size}&sort=nombre,asc`;

    // 🔥 Filtro por empresa (solo tiene efecto real para SUPER_ADMIN; el
    // backend lo ignora para el resto de los roles).
    if (empresaId) {
      url += `&empresaId=${empresaId}`;
    }

    // 🔥 Búsqueda por código o nombre (todos los roles).
    if (busqueda) {
      url += `&busqueda=${encodeURIComponent(busqueda)}`;
    }

    return this.http.get<Page<Repuesto>>(url);
  }

  create(repuesto: Repuesto): Observable<Repuesto> {
    return this.http.post<Repuesto>(`${this.apiUrl}/repuestos`, repuesto);
  }

  update(id: number, repuesto: Repuesto): Observable<Repuesto> {
    return this.http.put<Repuesto>(`${this.apiUrl}/repuestos/${id}`, repuesto);
  }

  delete(id: number): Observable<Repuesto> {
    return this.http.delete<Repuesto>(`${this.apiUrl}/repuestos/${id}`);
  }
}
