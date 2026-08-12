import { inject, Injectable } from '@angular/core';
import { environment } from '../../environments/environment';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SesionUsuarioResponse } from '../components/sesion-usuario/models/sesion-usuario.model';
import { Page } from '../shared/page';

@Injectable({
  providedIn: 'root'
})
export class SesionUsuarioService {

  private apiUrl = environment.apiUrl;
  private http = inject(HttpClient);

  actualizarActividad(
    pagina: string,
    accion: string
  ): Observable<void> {

    return this.http.post<void>(
      `${environment.apiUrl}/sesiones/actividad`,
      {
        pagina,
        accion
      }
    );
  }

  logout(tokenJti: string): Observable<void> {

    return this.http.post<void>(
      `${environment.apiUrl}/sesiones/logout/${tokenJti}`,
      {}
    );
  }

  obtenerSesionesActivas():
    Observable<SesionUsuarioResponse[]> {

    return this.http.get<SesionUsuarioResponse[]>(
      `${environment.apiUrl}/sesiones/activas`
    );
  }

  // 🔥 Informe de conexiones: historial paginado y filtrable por
  // usuario / empresa / fecha (solo SUPER_ADMIN puede consultarlo).
  obtenerHistorialConexiones(
    page: number,
    size: number,
    usuario?: string,
    empresaId?: number,
    fecha?: string
  ): Observable<Page<SesionUsuarioResponse>> {

    let params = new HttpParams()
      .set('page', page)
      .set('size', size);

    if (usuario) {
      params = params.set('usuario', usuario);
    }

    if (empresaId != null) {
      params = params.set('empresaId', empresaId);
    }

    if (fecha) {
      params = params.set('fecha', fecha);
    }

    return this.http.get<Page<SesionUsuarioResponse>>(
      `${environment.apiUrl}/sesiones/historial`,
      { params }
    );
  }
}
