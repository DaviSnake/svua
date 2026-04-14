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

  getAll(page = 0, size = 10): Observable<Page<Proveedor>> {
    return this.http.get<Page<Proveedor>>(`${this.apiUrl}/proveedores?page=${page}&size=${size}?page=0&size=3&sort=nombre,asc`);
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
