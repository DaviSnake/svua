import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { Client } from '@stomp/stompjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {

  private client!: Client;

  // 🔵 ESTADO INTERNO DEL SERVICIO
  private notificaciones: any[] = [];

  private noLeidasSubject = new BehaviorSubject<number>(0);
  noLeidas$ = this.noLeidasSubject.asObservable();

  conectar(empresaId: number, callback: (n: any) => void) {

    this.client = new Client({
      brokerURL: environment.wsUrl
    });

    this.client.onConnect = () => {

      this.client.subscribe(
        '/topic/notificaciones/' + empresaId,
        response => {

          const notificacion = JSON.parse(response.body);

          const index = this.notificaciones.findIndex(
            n => n.id === notificacion.id
          );

          if (index !== -1) {
            // 🔵 actualizar existente
            this.notificaciones[index] = notificacion;
          } else {
            // 🔵 insertar nueva
            this.notificaciones.unshift(notificacion);
          }

          this.emitirNoLeidas();
        }
      );
    };

    this.client.activate();
  }

  marcarComoLeida(notificacion: any) {
    notificacion.leida = true;
    this.emitirNoLeidas();
  }

  private emitirNoLeidas() {
    const count = this.notificaciones.filter(n => !n.leida).length;
    this.noLeidasSubject.next(count);
  }

  desconectar(): void {
    if (this.client?.active) {
      this.client.deactivate();
    }
  }
}