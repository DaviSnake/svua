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

  getAll(page = 0, size = 10): Observable<Page<Activo>> {
    return this.http.get<Page<Activo>>(`${this.apiUrl}/activos?page=${page}&size=${size}?page=0&size=3&sort=nombre,asc`);
  }

  create(activo: Activo): Observable<Activo> {
    return this.http.post<Activo>(`${this.apiUrl}/activos`, activo);
  }

  update(id: number, data: Activo) {
    return this.http.put(`${this.apiUrl}/activos/${id}`, data);
  }

  delete(id: number) {
    return this.http.delete(`${this.apiUrl}/activos/${id}`);
  }
}
