import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Page } from '../shared/page';
import { Proveedor } from '../model/proveedor';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ProveedorService {

  private apiUrl = environment.apiUrl;
  private http = inject(HttpClient);

  getAll(page = 0, size = 10, empresaId?: number | null, busqueda?: string | null): Observable<Page<Proveedor>> {
    let url = `${this.apiUrl}/proveedores?page=${page}&size=${size}&sort=nombre,asc`;

    // 🔥 Filtro por empresa (solo tiene efecto real para SUPER_ADMIN; el
    // backend lo ignora para el resto de los roles).
    if (empresaId) {
      url += `&empresaId=${empresaId}`;
    }

    // 🔥 Búsqueda por nombre o rut (todos los roles).
    if (busqueda) {
      url += `&busqueda=${encodeURIComponent(busqueda)}`;
    }

    return this.http.get<Page<Proveedor>>(url);
  }

  getProveedorCombo(page = 0, size = 50, empresaId?: number | null): Observable<Page<Proveedor>> {
    let url = `${this.apiUrl}/proveedores?page=${page}&size=${size}&sort=nombre,asc`;

    // 🔥 Filtro por empresa (solo tiene efecto real para SUPER_ADMIN; el
    // backend lo ignora para el resto de los roles).
    if (empresaId) {
      url += `&empresaId=${empresaId}`;
    }

    return this.http.get<Page<Proveedor>>(url);
  }

  create(proveedor: Proveedor): Observable<Proveedor> {
    return this.http.post<Proveedor>(`${this.apiUrl}/proveedores`, proveedor);
  }

  update(id: number, proveedor: Proveedor): Observable<Proveedor> {
      return this.http.put<Proveedor>(`${this.apiUrl}/proveedores/${id}`, proveedor);
    }
    
    delete(id: number): Observable<Proveedor> {
      return this.http.delete<Proveedor>(`${this.apiUrl}/proveedores/${id}`);
    }
}
