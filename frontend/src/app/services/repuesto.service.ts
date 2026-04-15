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

  getAll(page = 0, size = 10): Observable<Page<Repuesto>> {
    return this.http.get<Page<Repuesto>>(`${this.apiUrl}/repuestos?page=${page}&size=${size}?page=0&size=3&sort=nombre,asc`);
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
