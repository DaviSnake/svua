import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Usuario } from '../model/usuario';
import { Page } from '../shared/page';
import { environment } from '../../environments/environment';
import { PerfilUsuario } from '../model/perfilUsuario';

@Injectable({
  providedIn: 'root'
})
export class UsuarioService {

  private apiUrl = environment.apiUrl;
  
  http = inject(HttpClient);

  getAll(page = 0, size = 3, empresaId?: number | null): Observable<Page<Usuario>> {
    let url = `${this.apiUrl}/usuarios?page=${page}&size=${size}&sort=nombre,asc`;

    // 🔥 Filtro por empresa (solo tiene efecto real para SUPER_ADMIN; el
    // backend lo ignora para el resto de los roles).
    if (empresaId) {
      url += `&empresaId=${empresaId}`;
    }

    return this.http.get<Page<Usuario>>(url);
  }

  getPerfilUsurio(): Observable<PerfilUsuario> {
    return this.http.get<PerfilUsuario>(`${this.apiUrl}/usuarios/perfilUsuario`);
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
