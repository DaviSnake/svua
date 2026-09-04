import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { DispositivoEmpresa } from '../model/dispositivo-empresa';
import { Page } from '../shared/page';

// 🔒 Solo SUPER_ADMIN (ver DispositivoEmpresaController): que
// dispositivo fisico de monitoreo alimenta a que empresa, usado por el
// importador de lecturas de Control de Turno via correo.
@Injectable({
  providedIn: 'root'
})
export class DispositivoEmpresaService {

  private apiUrl = environment.apiUrl;
  private http = inject(HttpClient);

  getAll(page = 0, size = 10, empresaId?: number | null, busqueda?: string | null): Observable<Page<DispositivoEmpresa>> {
    let url = `${this.apiUrl}/control-turno/dispositivos?page=${page}&size=${size}&sort=codigoDispositivo,asc`;
    if (empresaId) url += `&empresaId=${empresaId}`;
    if (busqueda) url += `&busqueda=${encodeURIComponent(busqueda)}`;
    return this.http.get<Page<DispositivoEmpresa>>(url);
  }

  create(dispositivo: DispositivoEmpresa): Observable<DispositivoEmpresa> {
    return this.http.post<DispositivoEmpresa>(`${this.apiUrl}/control-turno/dispositivos`, dispositivo);
  }

  update(id: number, dispositivo: DispositivoEmpresa): Observable<DispositivoEmpresa> {
    return this.http.put<DispositivoEmpresa>(`${this.apiUrl}/control-turno/dispositivos/${id}`, dispositivo);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/control-turno/dispositivos/${id}`);
  }

  habilitar(id: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/control-turno/dispositivos/${id}/habilitar`, {});
  }
}
