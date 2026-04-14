import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { OrdenMantencion } from '../model/ordenMantencion';
import { of } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class OrdenMantencionService {

  private apiUrl = environment.apiUrl;
  private http = inject(HttpClient);

  listar() {
    //return of([] as Cita[]);
    return this.http.get<OrdenMantencion[]>(`${this.apiUrl}/ordenes-mantenimiento`);
  }

  crear(ordenMantencion: OrdenMantencion) {
    return this.http.post<OrdenMantencion>(`${this.apiUrl}/ordenes-mantenimiento`, ordenMantencion);
  }

  actualizar(id: number, ordenMantencion: OrdenMantencion) {
    return this.http.put(`${this.apiUrl}/ordenes-mantenimiento/${id}`, ordenMantencion);
  }

  eliminar(id: number) {
    return this.http.delete(`${this.apiUrl}/ordenes-mantenimiento/${id}`);
  }

}
