import { Injectable, NgZone, inject } from '@angular/core';
import { Router } from '@angular/router';
import Swal from 'sweetalert2';
import { AuthService } from './auth.service';
import { SesionUsuarioService } from './sesion-usuario.service';
import { WebSocketService } from './web-socket.service';
import { environment } from '../../environments/environment';

// 🔥 Tiempo máximo sin actividad del usuario (mouse, teclado, scroll,
// touch) antes de cerrar la sesión automáticamente por inactividad.
// Parametrizable en assets/env.js (tiempoInactividadMinutos) sin
// necesidad de recompilar el frontend; por defecto son 5 minutos.
const TIEMPO_INACTIVIDAD_MS = environment.tiempoInactividadMinutos * 60 * 1000;

const EVENTOS_ACTIVIDAD = ['mousemove', 'mousedown', 'keydown', 'scroll', 'touchstart', 'click'];

// 🔥 Cierra la sesión si no hay ninguna interacción del usuario en esta
// pestaña durante TIEMPO_INACTIVIDAD_MS. Se activa/desactiva desde
// LayoutComponent (solo corre mientras el usuario está dentro de la app,
// nunca en la pantalla de login).
@Injectable({
  providedIn: 'root'
})
export class InactivityService {

  private authService = inject(AuthService);
  private sesionUsuarioService = inject(SesionUsuarioService);
  private webSocketService = inject(WebSocketService);
  private router = inject(Router);
  private ngZone = inject(NgZone);

  private timeoutId?: ReturnType<typeof setTimeout>;
  private escuchando = false;
  private cerrandoPorInactividad = false;

  // Referencia estable para poder sacar el listener en detener().
  private readonly resetHandler = () => this.reiniciarTimer();

  /**
   * Empieza a vigilar la actividad del usuario en esta pestaña.
   */
  iniciar(): void {

    if (this.escuchando) {
      return;
    }

    this.escuchando = true;
    this.cerrandoPorInactividad = false;

    // 🔥 Los listeners de actividad corren fuera de Angular: un
    // mousemove no debe disparar detección de cambios en toda la app.
    // Solo se vuelve a entrar a la zona cuando de verdad hay que cerrar
    // la sesión.
    this.ngZone.runOutsideAngular(() => {
      EVENTOS_ACTIVIDAD.forEach(evento =>
        document.addEventListener(evento, this.resetHandler, { passive: true })
      );
    });

    this.reiniciarTimer();
  }

  /**
   * Deja de vigilar (logout manual, o al salir del layout autenticado).
   */
  detener(): void {

    if (!this.escuchando) {
      return;
    }

    this.escuchando = false;

    EVENTOS_ACTIVIDAD.forEach(evento =>
      document.removeEventListener(evento, this.resetHandler)
    );

    this.limpiarTimer();
  }

  private reiniciarTimer(): void {

    this.limpiarTimer();

    this.timeoutId = setTimeout(() => {
      this.ngZone.run(() => this.cerrarPorInactividad());
    }, TIEMPO_INACTIVIDAD_MS);
  }

  private limpiarTimer(): void {

    if (this.timeoutId) {
      clearTimeout(this.timeoutId);
      this.timeoutId = undefined;
    }
  }

  /**
   * Cierra la sesión igual que el botón "Salir" del menú (marca la
   * sesión como cerrada en el backend, para que no quede "activa" en el
   * informe de Sesiones Activas), avisando al usuario que fue por
   * inactividad.
   */
  private cerrarPorInactividad(): void {

    if (this.cerrandoPorInactividad) {
      return;
    }

    this.cerrandoPorInactividad = true;

    this.detener();
    this.webSocketService.desconectar();

    const finalizar = () => {

      this.authService.finalizarSesionLocal();

      Swal.fire({
        icon: 'info',
        title: 'Sesión cerrada',
        text: 'Tu sesión se cerró automáticamente por inactividad.',
        confirmButtonText: 'Ir a iniciar sesión'
      }).then(() => {
        this.router.navigateByUrl('/login');
      });
    };

    const tokenJti = this.authService.getTokenJti();

    if (!tokenJti) {
      finalizar();
      return;
    }

    // Avisa al backend que esta sesión terminó (igual que el logout
    // manual), para que el informe de Sesiones Activas quede correcto.
    // Si cualquiera de las dos llamadas falla (red caída, token ya
    // vencido, etc.) igual se termina la sesión local.
    this.sesionUsuarioService.logout(tokenJti).subscribe({
      next: () => this.authService.logout().subscribe({ next: finalizar, error: finalizar }),
      error: () => this.authService.logout().subscribe({ next: finalizar, error: finalizar })
    });
  }
}
