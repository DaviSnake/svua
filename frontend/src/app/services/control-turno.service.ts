import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Page } from '../shared/page';
import { PuntoControl } from '../model/punto-control';
import { ImportHojaControlResponse, LecturaControl, PuntoControlDashboard, TurnoTrabajo } from '../model/lectura-control';
import { environment } from '../../environments/environment';

// 🔥 Modulo "Control de Turno": puntos de control (temperatura,
// humedad, etc.) monitoreados manualmente cada turno, sus lecturas
// horarias y el dashboard de graficos que las resume. Reemplaza el
// registro en planilla Excel (SISTEMA_DE_CONTROL_DE_MANTENCION).
@Injectable({
  providedIn: 'root'
})
export class ControlTurnoService {

  private apiUrl = `${environment.apiUrl}/control-turno`;
  private http = inject(HttpClient);

  // ---------------- Puntos de control (catalogo) ----------------

  getPuntos(page = 0, size = 10, empresaId?: number | null, busqueda?: string | null): Observable<Page<PuntoControl>> {
    let url = `${this.apiUrl}/puntos?page=${page}&size=${size}&sort=nombre,asc`;

    if (empresaId) {
      url += `&empresaId=${empresaId}`;
    }

    if (busqueda) {
      url += `&busqueda=${encodeURIComponent(busqueda)}`;
    }

    return this.http.get<Page<PuntoControl>>(url);
  }

  // 🔥 Combo simple (sin paginar) de puntos activos de la empresa del
  // usuario actual, usado en el formulario de registro de lecturas.
  getPuntosActivos(): Observable<PuntoControl[]> {
    return this.http.get<PuntoControl[]>(`${this.apiUrl}/puntos/activos`);
  }

  crearPunto(punto: PuntoControl): Observable<PuntoControl> {
    return this.http.post<PuntoControl>(`${this.apiUrl}/puntos`, punto);
  }

  actualizarPunto(id: number, punto: PuntoControl): Observable<PuntoControl> {
    return this.http.put<PuntoControl>(`${this.apiUrl}/puntos/${id}`, punto);
  }

  eliminarPunto(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/puntos/${id}`);
  }

  habilitarPunto(id: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/puntos/${id}/habilitar`, null);
  }

  // ---------------- Lecturas ----------------

  registrarLectura(lectura: Partial<LecturaControl>): Observable<LecturaControl> {
    return this.http.post<LecturaControl>(`${this.apiUrl}/lecturas`, lectura);
  }

  // 🔥 Carga masiva desde la planilla real "HOJA DE CONTROL": crea las
  // lecturas de HOY para cada punto/hora que trae el archivo.
  importarExcel(archivo: File): Observable<ImportHojaControlResponse> {

    const formData = new FormData();
    formData.append('file', archivo);

    return this.http.post<ImportHojaControlResponse>(
      `${this.apiUrl}/lecturas/importar-excel`,
      formData
    );
  }

  getLecturas(
    page = 0,
    size = 10,
    puntoControlId?: number | null,
    desde?: string | null,
    hasta?: string | null,
    turno?: TurnoTrabajo | null
  ): Observable<Page<LecturaControl>> {

    let url = `${this.apiUrl}/lecturas?page=${page}&size=${size}&sort=fechaHora,desc`;

    if (puntoControlId) { url += `&puntoControlId=${puntoControlId}`; }
    if (desde) { url += `&desde=${desde}`; }
    if (hasta) { url += `&hasta=${hasta}`; }
    if (turno) { url += `&turno=${turno}`; }

    return this.http.get<Page<LecturaControl>>(url);
  }

  getDashboard(
    puntoControlId?: number | null,
    desde?: string | null,
    hasta?: string | null,
    turno?: TurnoTrabajo | null
  ): Observable<PuntoControlDashboard[]> {

    const params: string[] = [];

    if (puntoControlId) { params.push(`puntoControlId=${puntoControlId}`); }
    if (desde) { params.push(`desde=${desde}`); }
    if (hasta) { params.push(`hasta=${hasta}`); }
    if (turno) { params.push(`turno=${turno}`); }

    const query = params.length ? `?${params.join('&')}` : '';

    return this.http.get<PuntoControlDashboard[]>(`${this.apiUrl}/lecturas/dashboard${query}`);
  }
}
