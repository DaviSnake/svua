import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Activo } from '../model/activo';
import { Observable } from 'rxjs';
import { Page } from '../shared/page';

@Injectable({
  providedIn: 'root'
})
export class ActivoService {

  private apiUrl = "http://localhost:8080/api/v1/svua/activos";
  private http = inject(HttpClient);

  getAll(page = 0, size = 10): Observable<Page<Activo>> {
    return this.http.get<Page<Activo>>(`${this.apiUrl}?page=${page}&size=${size}?page=0&size=3&sort=nombre,asc`);
  }

  create(activo: Activo): Observable<Activo> {
    return this.http.post<Activo>(this.apiUrl, activo);
  }

  update(id: number, data: Activo) {
    return this.http.put(`${this.apiUrl}/${id}`, data);
  }

  delete(id: number) {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }
}
