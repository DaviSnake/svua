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

  getAll(page = 0, size = 10): Observable<Page<TipoActivo>> {
    return this.http.get<Page<TipoActivo>>(`${this.apiUrl}/tipos-activo?page=${page}&size=${size}?page=0&size=3&sort=nombre,asc`);
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
