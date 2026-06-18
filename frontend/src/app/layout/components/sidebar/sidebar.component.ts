import { Component, HostListener, OnDestroy, OnInit, inject } from '@angular/core';
import { Router, RouterLink, RouterModule, NavigationEnd } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { CommonModule } from '@angular/common';
import { filter } from 'rxjs/operators';
import { NotificacionService } from '../../../services/notificacion.service';
import { interval, Subscription } from 'rxjs';
import { NotificacionStateService } from '../../../services/notificacion-state.service';
import { SesionUsuarioService } from '../../../services/sesion-usuario.service';
import { WebSocketService } from '../../../services/web-socket.service';
import { Notificacion } from '../../../model/notificacion';
import Swal from 'sweetalert2';

type MenuKey = 'gestion' | 'organizacion' | 'analisis';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterModule, CommonModule],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css'
})
export class SidebarComponent implements OnInit, OnDestroy  {

  authService = inject(AuthService);
  notificacionService = inject(NotificacionService);
  notificacionState = inject(NotificacionStateService);
  sesionUsuarioService = inject(SesionUsuarioService);
  webSocketService = inject(WebSocketService);
  router = inject(Router);

  usuario: any;

  // 🔥 estado sidebar
  isCollapsed = false;

  // 🔥 estado menús
  openMenus: Record<MenuKey, boolean> = {
    gestion: false,
    organizacion: false,
    analisis: false
  };

  // roles
  esAdmin = false;
  esAdminEmpresa = false;
  esDemo = false;

  rutaActual = '';

  notificaciones: Notificacion[] = [];

  totalNotificaciones = 0;

  empresaId: number = 0;
  cantidadNoLeidas = 0;

  private intervaloNotificaciones?: Subscription;
  
  ngOnInit() {
    this.empresaId = this.authService.getEmpresaId() ?? 0;

    // usuario
    this.authService.user$.subscribe(user => {
      this.usuario = user;
      this.esAdmin = this.authService.isAdmin();
      this.esAdminEmpresa = this.authService.isAdminEmpresa();
      this.esDemo = this.authService.getDemo()!;
    });

    // 🔥 abrir menú según ruta
    this.detectarRuta(this.router.url);

    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe((event: any) => {
        this.detectarRuta(event.url);
      });

      /*this.cargarCantidadNoLeidas(this.empresaId);
      this.intervaloNotificaciones =
        interval(30000).subscribe(() => {
          this.cargarCantidadNoLeidas(this.empresaId);
      });*/

      this.notificacionState.actualizarCantidad$
      .subscribe(() => {
        this.cargarCantidadNoLeidas(this.empresaId);
      });

      this.webSocketService.conectar(

        this.authService.getEmpresaId()!,

        notificacion => {

            this.notificaciones.unshift(
                notificacion);

            this.totalNotificaciones++;

            this.notificacionState.notificarActualizacion();

        });

  }

  ngOnDestroy(): void {

    this.intervaloNotificaciones?.unsubscribe();

  }

  // 🔥 detectar menú activo por URL
  detectarRuta(url: string) {

    this.openMenus = {
      gestion: false,
      organizacion: false,
      analisis: false
    };

    if (
      url.includes('/activo') ||
      url.includes('/tipoActivo') ||
      url.includes('/repuesto')
    ) {
      this.openMenus.gestion = true;
    }

    if (
      url.includes('/empresa') ||
      url.includes('/ubicacion') ||
      url.includes('/bodega') ||
      url.includes('/proveedor')
    ) {
      this.openMenus.organizacion = true;
    }

    if (
      url.includes('/reportes') ||
      url.includes('/auditorias')
    ) {
      this.openMenus.analisis = true;
    }
  }

  // 🔒 cerrar todo
  closeAllMenus() {
    this.openMenus = {
      gestion: false,
      organizacion: false,
      analisis: false
    };
  }

  // 🔥 toggle menú (BLOQUEADO si está colapsado)
  toggleMenu(menu: MenuKey) {

    if (this.isCollapsed) return; // 🔥 CLAVE

    Object.keys(this.openMenus).forEach((key) => {
      const k = key as MenuKey;
      this.openMenus[k] = k === menu ? !this.openMenus[k] : false;
    });
  }

  // 🔥 toggle sidebar
  toggleSidebar() {
    this.isCollapsed = !this.isCollapsed;
  }

  cargarCantidadNoLeidas(empresaId: number): void {
    this.notificacionService
      .obtenerCantidadNoLeidas(empresaId)
      .subscribe({
        next: cantidad => {
          this.cantidadNoLeidas = cantidad;
        }
      });
  }

  logout() {
    this.sesionUsuarioService.logout(this.authService.getTokenJti()!).subscribe({
      next: () => {

        this.authService.logout().subscribe({
           next: () => {
             this.authService.clearSession();     
             this.router.navigate(['/login']);
           },
           error: (err) => {
             console.error('Error al cerrar sesión', err);
             this.authService.clearSession();
             this.router.navigate(['/login']);
           }
         });
      }
    });

    this.intervaloNotificaciones?.unsubscribe();
  }
}