import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Page } from '../shared/page';
import { Proveedor } from '../model/proveedor';

@Injectable({
  providedIn: 'root'
})
export class ProveedorService {

  private apiUrl = "http://localhost:8080/api/v1/svua/proveedores";
  private http = inject(HttpClient);

  getAll(page = 0, size = 10): Observable<Page<Proveedor>> {
    return this.http.get<Page<Proveedor>>(`${this.apiUrl}?page=${page}&size=${size}?page=0&size=3&sort=nombre,asc`);
  }

  create(proveedor: Proveedor): Observable<Proveedor> {
    return this.http.post<Proveedor>(this.apiUrl, proveedor);
  }

  update(id: number, proveedor: Proveedor): Observable<Proveedor> {
      return this.http.put<Proveedor>(`${this.apiUrl}/${id}`, proveedor);
    }
    
    delete(id: number): Observable<Proveedor> {
      return this.http.delete<Proveedor>(`${this.apiUrl}/${id}`);
    }
}
