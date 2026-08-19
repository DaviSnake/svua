import { Component, ElementRef, inject, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { HeaderComponent } from "./components/header/header.component";
import { SidebarComponent } from "./components/sidebar/sidebar.component";
import { Router, RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { NotificacionStateService } from '../services/notificacion-state.service';
import { AuthService } from '../services/auth.service';
import { NotificacionService } from '../services/notificacion.service';
import { WebSocketService } from '../services/web-socket.service';
import { InactivityService } from '../services/inactivity.service';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [SidebarComponent, RouterOutlet, CommonModule],
  templateUrl: './layout.component.html',
  styleUrl: './layout.component.css'
})
export class LayoutComponent implements OnInit, OnDestroy {

  notificacionState = inject(NotificacionStateService);
  authService = inject(AuthService);
  notificacionService = inject(NotificacionService);
  webSocketService = inject(WebSocketService);
  inactivityService = inject(InactivityService);
  router = inject(Router);

  @ViewChild('sidebarPadre') sidebarPadre!: ElementRef;

  sidebarOpen = false;

  empresaId: number = 0;
  cantidadNoLeidas = 0;

  ngOnInit(){

    this.empresaId = this.authService.getEmpresaId() ?? 0;

    // 🔥 el conteo de no leidas se cargaba SOLO desde
    // WebSocketService.noLeidas$, un contador en memoria que arranca
    // en 0 y solo suma mensajes recibidos por socket durante esta
    // sesión — las notificaciones no leídas de antes de entrar a la
    // app nunca se reflejaban. Ahora se pide el conteo real a la BD
    // al iniciar, y se vuelve a pedir cada vez que llega un mensaje
    // nuevo por socket o cuando se marca algo como leído/se elimina
    // (NotificacionStateService.actualizarCantidad$).
    this.cargarCantidadNoLeidas();

    this.webSocketService.noLeidas$.subscribe(() => {
      this.cargarCantidadNoLeidas();
    });

    this.notificacionState.actualizarCantidad$.subscribe(() => {
      this.cargarCantidadNoLeidas();
    });

    // 🔥 Cierra la sesión si no hay actividad del usuario (mouse,
    // teclado, scroll) durante 5 minutos. Solo corre mientras está
    // dentro del layout autenticado.
    this.inactivityService.iniciar();

  }

  private cargarCantidadNoLeidas(): void {
    if (!this.empresaId) {
      return;
    }

    this.notificacionService
      .contarNoLeidas(this.empresaId)
      .subscribe(count => {
        this.cantidadNoLeidas = count;
      });
  }

  ngOnDestroy(): void {
    this.inactivityService.detener();
  }

  abrirNotificaciones() {

      this.router.navigateByUrl('/inicio/notificaciones');

  }

  toggleSidebar() {
    this.sidebarOpen = !this.sidebarOpen;
  }

  closeSidebar() {
    this.sidebarOpen = false;
  }
  
  menuBtnClick(flag: string): void {
    if (flag !== "1") {
      this.sidebarPadre.nativeElement.classList.remove('minimize');
    } else {
      this.sidebarPadre.nativeElement.classList.add('minimize');

    }
  }

}
