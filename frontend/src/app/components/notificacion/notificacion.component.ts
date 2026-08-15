import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { Notificacion } from '../../model/notificacion';
import { CommonModule } from '@angular/common';
import { NotificacionService } from '../../services/notificacion.service';
import { Router } from '@angular/router';
import { NotificacionStateService } from '../../services/notificacion-state.service';
import { WebSocketService } from '../../services/web-socket.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-notificacion',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notificacion.component.html',
  styleUrl: './notificacion.component.css'
})
export class NotificacionComponent implements OnInit, OnDestroy {

  notificacionService = inject(NotificacionService);
  notificacionStateService = inject(NotificacionStateService);
  webSocketService = inject(WebSocketService);
  router = inject(Router);

  notificaciones: Notificacion[] = [];

  // 🔥 notificaciones$ es un Subject/BehaviorSubject compartido: guardamos
  // la suscripción para no acumular una nueva cada vez que se llama
  // cargarNotificaciones() (antes cada refresco dejaba una suscripción
  // activa adicional, sin liberarla nunca).
  private notificacionesSub?: Subscription;

  ngOnInit(): void {
    this.cargarNotificaciones();
  }

  ngOnDestroy(): void {
    this.notificacionesSub?.unsubscribe();
  }

  cargarNotificaciones(): void {

    this.notificacionesSub?.unsubscribe();

    this.notificacionesSub = this.notificacionService.notificaciones$.subscribe(data => {
    this.notificaciones = data.map(n => ({
      ...n,
      expandida: false
    }));
  });

    this.notificacionService.listarNotificaciones();
  }

  abrirNotificacion(n: Notificacion): void {

    // marcar como leída
    this.notificacionService
    .marcarComoLeida(n.id)
    .subscribe({
      next: () => {
        this.cargarNotificaciones();
        this.webSocketService.marcarComoLeida(n);
      },
      error: (error) => {
        console.error('Error al marcar notificación como leída', error);
      }
    });

  }

  eliminarNotificacion(event: MouseEvent, n: Notificacion): void {

    event.stopPropagation();

    this.notificacionService
      .eliminar(n.id)
      .subscribe(() => {

        this.cargarNotificaciones();

        this.notificacionStateService
          .notificarActualizacion();

      });
  }

  // 🔥 trackBy para la lista de notificaciones.
  trackByNotificacionId(index: number, n: any): any {
    return n?.id ?? index;
  }

  toggleMensaje(n: any): void {

    n.expandida = !n.expandida;

    if (!n.leida) {

      this.notificacionService
        .marcarComoLeida(n.id)
        .subscribe(() => {

          n.leida = true;

          this.notificacionStateService.notificarActualizacion();
        });
    }
  }

}
