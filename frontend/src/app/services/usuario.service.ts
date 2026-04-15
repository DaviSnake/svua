import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Usuario } from '../model/usuario';
import { Page } from '../shared/page';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class UsuarioService {

  private apiUrl = environment.apiUrl;
  
  http = inject(HttpClient);

  getAll(page = 0, size = 3): Observable<Page<Usuario>> {
    return this.http.get<Page<Usuario>>(`${this.apiUrl}/usuarios?page=${page}&size=${size}?page=0&size=3&sort=nombre,asc`);
  }

  create(usuario: Usuario): Observable<Usuario> {
    return this.http.post<Usuario>(`${this.apiUrl}/usuarios`, usuario);
  }

  update(id: number, usuario: Usuario): Observable<Usuario> {
    return this.http.put<Usuario>(`${this.apiUrl}/usuarios/${id}`, usuario);
  }

  delete(id: number): Observable<Usuario> {
    return this.http.delete<Usuario>(`${this.apiUrl}/usuarios/${id}`);
  }

  cambiarPassword(id: number, data: any): Observable<Usuario> {
    return this.http.put<Usuario>(`${this.apiUrl}/usuarios/${id}/password`, data);
  }

}
