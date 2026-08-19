import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { OrdenRepuesto } from '../model/ordenRepuesto';

export interface OrdenRepuestoRequest {
  ordenId: number;
  repuestoId: number;
  cantidad: number;
  costoUnitario: number;
}

@Injectable({
  providedIn: 'root'
})
export class OrdenRepuestoService {

  private apiUrl = environment.apiUrl;
  private http = inject(HttpClient);

  // 🔥 agrega un repuesto/fungible a una orden YA EXISTENTE, guardando de
  // inmediato en la BD (descuenta stock al instante, sin esperar al
  // botón "Actualizar" que ya no existe para este flujo).
  agregar(request: OrdenRepuestoRequest): Observable<OrdenRepuesto> {
    return this.http.post<OrdenRepuesto>(`${this.apiUrl}/orden-repuestos`, request);
  }

  // 🔥 elimina un repuesto ya guardado, reponiendo el stock que se
  // había descontado al agregarlo.
  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/orden-repuestos/${id}`);
  }

  listarPorOrden(ordenId: number): Observable<OrdenRepuesto[]> {
    return this.http.get<OrdenRepuesto[]>(`${this.apiUrl}/orden-repuestos/orden/${ordenId}`);
  }
}
