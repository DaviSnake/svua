import { Component, inject, OnInit } from '@angular/core';
import { Notificacion } from '../../model/notificacion';
import { CommonModule } from '@angular/common';
import { NotificacionService } from '../../services/notificacion.service';
import { Router } from '@angular/router';
import { NotificacionStateService } from '../../services/notificacion-state.service';

@Component({
  selector: 'app-notificacion',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notificacion.component.html',
  styleUrl: './notificacion.component.css'
})
export class NotificacionComponent implements OnInit {

  notificacionService = inject(NotificacionService);
  notificacionState = inject(NotificacionStateService);
  router = inject(Router);

  notificaciones: Notificacion[] = [];

  ngOnInit(): void {
    this.cargarNotificaciones();
  }

  cargarNotificaciones(): void {
    this.notificacionService.listarNotificaciones()
      .subscribe({
        next: data => {
          this.notificaciones = data;
        }
      });
  }

  abrirNotificacion(n: Notificacion): void {

    // marcar como leída
    this.notificacionService
    .marcarComoLeida(n.id)
    .subscribe({
      next: () => {
        this.cargarNotificaciones();
        this.notificacionState.notificarActualizacion();
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

        this.notificacionState
          .notificarActualizacion();

      });
  }

}
