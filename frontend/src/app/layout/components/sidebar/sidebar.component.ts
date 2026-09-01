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

type MenuKey = 'gestion' | 'organizacion' | 'analisis' | 'controlTurno';

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
    analisis: false,
    controlTurno: false
  };

  // roles
  esAdmin = false;
  esAdminEmpresa = false;
  esTecnico = false;
  esDemo = false;
  // 🔥 Control de Turno: visible para SUPER_ADMIN/ADMIN_EMPRESA/
  // JEFE_MANTENIMIENTO/TECNICO (ver ROLES_CONTROL_TURNO en
  // control-turno-acceso.guard.ts) -- a diferencia de las secciones de
  // "gestion", aqui SI debe verlo el TECNICO. Ademas del rol, ahora
  // TAMBIEN exige Empresa.controlTurnoHabilitado (V33): antes cualquier
  // empresa con un usuario de esos roles veia el menu, use o no el
  // modulo.
  mostrarControlTurno = false;
  codigoQrHabilitado = false; // 🔒 controla el link "Escanear Activo"
  codigoEan13Habilitado = false;
  informeMantencionesHabilitado = false; // 🔒 controla el link "Informe de Mantenciones" (ADMIN_EMPRESA)

  // © footer del sidebar
  anioActual = new Date().getFullYear();

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
      this.esTecnico = this.authService.isTecnico()!;
      this.esAdminEmpresa = this.authService.isAdminEmpresa();
      // 🔥 esAdmin (SUPER_ADMIN) bypasea el flag de empresa, igual que
      // con codigoQrHabilitado/codigoEan13Habilitado mas abajo -- puede
      // administrar/revisar el modulo aunque la empresa todavia no lo
      // tenga habilitado.
      this.mostrarControlTurno = this.esAdmin || (
        ['SUPER_ADMIN', 'ADMIN_EMPRESA', 'JEFE_MANTENIMIENTO', 'TECNICO']
          .includes(this.authService.getUserRole() ?? '')
        && !!this.authService.getControlTurnoHabilitado()
      );
      this.esDemo = this.authService.getDemo()!;
      this.codigoQrHabilitado = this.authService.getCodigoQrHabilitado() ?? false;
      this.codigoEan13Habilitado = this.authService.getCodigoEan13Habilitado() ?? false;
      this.informeMantencionesHabilitado = this.authService.getInformeMantencionesHabilitado() ?? false;
    });

    // 🔥 abrir menú según ruta
    this.detectarRuta(this.router.url);

    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe((event: any) => {
        this.detectarRuta(event.url);
      });

      // 🔥 el conteo de no leidas se calculaba SOLO desde este array
      // en memoria (poblado únicamente por mensajes recibidos por
      // socket en esta sesión), así que las notificaciones no leídas
      // de antes de entrar a la app nunca se reflejaban. Ahora se pide
      // el conteo real a la BD al iniciar, cada 30s, cuando llega algo
      // nuevo por socket, y cuando se marca como leído/se elimina.
      this.cargarCantidadNoLeidas();
      this.intervaloNotificaciones =
        interval(30000).subscribe(() => {
          this.cargarCantidadNoLeidas();
      });

      this.notificacionState.actualizarCantidad$
      .subscribe(() => {
        this.cargarCantidadNoLeidas();
      });

      this.webSocketService.conectar(

        this.authService.getEmpresaId()!,

        notificacion => {

            this.notificaciones.unshift(
                notificacion);

            this.cargarCantidadNoLeidas();

        });

        this.webSocketService.noLeidas$.subscribe(() => {
          this.cargarCantidadNoLeidas();
        });

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

    this.intervaloNotificaciones?.unsubscribe();

    this.webSocketService.desconectar();

  }


  // 🔥 detectar menú activo por URL
  detectarRuta(url: string) {

    this.openMenus = {
      gestion: false,
      organizacion: false,
      analisis: false,
      controlTurno: false
    };

    if (
      url.includes('/activo') ||
      url.includes('/tipoActivo') ||
      url.includes('/repuesto') ||
      url.includes('/depreciacionAcelerada')
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
      url.includes('/auditorias') ||
      url.includes('/informeMantenciones')
    ) {
      this.openMenus.analisis = true;
    }

    if (url.includes('/controlTurno')) {
      this.openMenus.controlTurno = true;
    }
  }

  // 🔒 cerrar todo
  closeAllMenus() {
    this.openMenus = {
      gestion: false,
      organizacion: false,
      analisis: false,
      controlTurno: false
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

  logout() {
    this.webSocketService.desconectar();

    this.sesionUsuarioService.logout(this.authService.getTokenJti()!).subscribe({
      next: () => {

        this.authService.logout().subscribe({
           next: () => {
             this.authService.finalizarSesionLocal();
             this.router.navigate(['/login']);
           },
           error: (err) => {
             console.error('Error al cerrar sesión', err);
             this.authService.finalizarSesionLocal();
             this.router.navigate(['/login']);
           }
         });
      }
    });

    this.intervaloNotificaciones?.unsubscribe();
  }
}
