import { Injectable } from '@angular/core';

import { Client } from '@stomp/stompjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {

  private client!: Client;

  conectar(
      empresaId: number,
      callback: (n:any)=>void) {

    this.client = new Client({
      brokerURL: environment.wsUrl
    });

    

    this.client.onConnect = () => {

      this.client.subscribe(

        '/topic/notificaciones/' + empresaId,

        response => {

          callback(JSON.parse(response.body));

        }

      );

    };

    this.client.activate();

  }

}