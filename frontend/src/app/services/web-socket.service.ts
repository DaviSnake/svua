import { Injectable } from '@angular/core';

import { Client } from '@stomp/stompjs';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {

  private client!: Client;

  conectar(
      empresaId: number,
      callback: (n:any)=>void) {

    this.client = new Client({
      brokerURL: 'ws://localhost:8080/ws'
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