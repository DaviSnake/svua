import { inject, Injectable } from '@angular/core';

import { environment } from '../../environments/environment';
import { HttpClient } from '@angular/common/http';
import { Notificacion } from '../model/notificacion';
import { BehaviorSubject, tap } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class NotificacionService {

  private notificacionesSubject = new BehaviorSubject<Notificacion[]>([]);
  notificaciones$ = this.notificacionesSubject.asObservable();

  private apiUrl = environment.apiUrl;
  private http = inject(HttpClient);

  listarNotificaciones() {
    return this.http.get<Notificacion[]>(`${this.apiUrl}/notificacion`)
      .subscribe(data => {
        this.notificacionesSubject.next(data);
      });
  }

  // 🔥 el conteo de no leidas dependia solo de mensajes recibidos por
  // websocket en la sesion actual (nunca se hidrataba desde la BD al
  // cargar la pagina), asi que las notificaciones no leidas de antes
  // de abrir la app no aparecian en el badge/menu.
  contarNoLeidas(empresaId: number) {
    return this.http.get<number>(
      `${this.apiUrl}/notificacion/no-leidas/count/${empresaId}`
    );
  }

  marcarComoLeida(id: number) {
    return this.http.put(
      `${this.apiUrl}/notificacion/${id}/leer`, {}
    ).pipe(
      tap(() => {
        const actual = this.notificacionesSubject.value;

        const updated = actual.map(n =>
          n.id === id ? { ...n, leida: true } : n
        );

        this.notificacionesSubject.next(updated);
      })
    );
  }

  eliminar(id: number) {
    return this.http.delete(`${this.apiUrl}/notificacion/${id}`).pipe(
      tap(() => {
        const updated = this.notificacionesSubject.value
          .filter(n => n.id !== id);

        this.notificacionesSubject.next(updated);
      })
    );
  }
  
}
