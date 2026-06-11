import { inject, Injectable } from '@angular/core';
import { environment } from '../../environments/environment';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SesionUsuarioResponse } from '../components/sesion-usuario/models/sesion-usuario.model';

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
}
