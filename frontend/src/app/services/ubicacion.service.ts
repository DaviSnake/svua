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

  getAll(page = 0, size = 10): Observable<Page<Ubicacion>> {
    return this.http.get<Page<Ubicacion>>(`${this.apiUrl}/ubicaciones?page=${page}&size=${size}?page=0&size=3&sort=nombre,asc`);
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
