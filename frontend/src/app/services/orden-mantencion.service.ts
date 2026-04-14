import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { OrdenMantencion } from '../model/ordenMantencion';
import { of } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class OrdenMantencionService {

  private API = "http://localhost:8080/api/v1/svua/ordenes-mantenimiento";
  private http = inject(HttpClient);

  listar() {
    //return of([] as Cita[]);
    return this.http.get<OrdenMantencion[]>(this.API);
  }

  crear(ordenMantencion: OrdenMantencion) {
    return this.http.post<OrdenMantencion>(this.API, ordenMantencion);
  }

  actualizar(id: number, ordenMantencion: OrdenMantencion) {
    return this.http.put(`${this.API}/${id}`, ordenMantencion);
  }

  eliminar(id: number) {
    return this.http.delete(`${this.API}/${id}`);
  }

}
