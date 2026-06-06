import { inject, Injectable } from '@angular/core';

import { environment } from '../../environments/environment';
import { HttpClient } from '@angular/common/http';
import { Notificacion } from '../model/notificacion';

@Injectable({
  providedIn: 'root'
})
export class NotificacionService {

  private apiUrl = environment.apiUrl;
  private http = inject(HttpClient);

  listarNotificaciones() {
    return this.http.get<Notificacion[]>(`${this.apiUrl}/notificacion`);
  }

  marcarComoLeida(id: number) {
    return this.http.put(
      `${this.apiUrl}/notificacion/${id}/leer`,
      {}
    );
  }

  obtenerCantidadNoLeidas(empresaId: number) {
    return this.http.get<number>(
      `${environment.apiUrl}/notificacion/no-leidas/count/${empresaId}`
    );
  }

  eliminar(id: number) {
    return this.http.delete(
      `${environment.apiUrl}/notificacion/${id}`
    );
  }
  
}
