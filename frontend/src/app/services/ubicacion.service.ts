import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Page } from '../shared/page';
import { Ubicacion } from '../model/ubicacion';

@Injectable({
  providedIn: 'root'
})
export class UbicacionService {

  private apiUrl = "http://localhost:8080/api/v1/svua/ubicaciones";
  private http = inject(HttpClient);

  getAll(page = 0, size = 10): Observable<Page<Ubicacion>> {
    return this.http.get<Page<Ubicacion>>(`${this.apiUrl}?page=${page}&size=${size}?page=0&size=3&sort=nombre,asc`);
  }

  create(ubicacion: Ubicacion): Observable<Ubicacion> {
    return this.http.post<Ubicacion>(this.apiUrl, ubicacion);
  }

  update(id: number, ubicacion: Ubicacion): Observable<Ubicacion> {
    return this.http.put<Ubicacion>(`${this.apiUrl}/${id}`, ubicacion);
  }
  
  delete(id: number): Observable<Ubicacion> {
    return this.http.delete<Ubicacion>(`${this.apiUrl}/${id}`);
  }
}
