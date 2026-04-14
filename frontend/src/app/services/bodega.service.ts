import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Bodega } from '../model/bodega';
import { Observable } from 'rxjs';
import { Page } from '../shared/page';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class BodegaService {

  private apiUrl = environment.apiUrl;
  private http = inject(HttpClient);

  getAll(page = 0, size = 10): Observable<Page<Bodega>> {
    return this.http.get<Page<Bodega>>(`${this.apiUrl}/bodegas?page=${page}&size=${size}?page=0&size=3&sort=nombre,asc`);
  }

  getId(id: number): Observable<Bodega> {
    return this.http.get<Bodega>(`${this.apiUrl}/bodegas/${id}`);
  }

  create(bodega: Bodega): Observable<Bodega> {
    return this.http.post<Bodega>(`${this.apiUrl}/bodegas`, bodega);
  }

  update(id: number, bodega: Bodega): Observable<Bodega> {
    return this.http.put<Bodega>(`${this.apiUrl}/bodegas/${id}`, bodega);
  }

  delete(id: number): Observable<Bodega> {
    return this.http.delete<Bodega>(`${this.apiUrl}/bodegas/${id}`);
  }
}
