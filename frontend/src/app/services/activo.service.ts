import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Activo } from '../model/activo';
import { ActivoEscaneoResponse } from '../model/activoEscaneo';
import { Observable } from 'rxjs';
import { Page } from '../shared/page';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ActivoService {

  private apiUrl = environment.apiUrl;
  private http = inject(HttpClient);

  getAll(page = 0, size = 10, empresaId?: number | null, busqueda?: string | null): Observable<Page<Activo>> {
    let url = `${this.apiUrl}/activos?page=${page}&size=${size}&sort=nombre,asc`;

    // 🔥 Filtro por empresa (solo tiene efecto real para SUPER_ADMIN; el
    // backend lo ignora para el resto de los roles).
    if (empresaId) {
      url += `&empresaId=${empresaId}`;
    }

    // 🔍 Busqueda por codigo interno o nombre (mantenedor de Activo).
    if (busqueda) {
      url += `&busqueda=${encodeURIComponent(busqueda)}`;
    }

    return this.http.get<Page<Activo>>(url);
  }

  getActivoCombo(page = 0, size = 200, empresaId?: number | null): Observable<Page<Activo>> {
    let url = `${this.apiUrl}/activos?page=${page}&size=${size}&sort=nombre,asc`;

    // 🔥 Filtro por empresa (solo tiene efecto real para SUPER_ADMIN; el
    // backend lo ignora para el resto de los roles).
    if (empresaId) {
      url += `&empresaId=${empresaId}`;
    }

    return this.http.get<Page<Activo>>(url);
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

  // 🔳 Escaneo de QR/EAN13: busca el activo por el codigo leido (con la
  // camara o con un lector fisico) y trae su historial de mantenciones.
  buscarPorCodigo(codigo: string): Observable<ActivoEscaneoResponse> {
    const params = new HttpParams().set('codigo', codigo);
    return this.http.get<ActivoEscaneoResponse>(`${this.apiUrl}/activos/escanear`, { params });
  }
}
