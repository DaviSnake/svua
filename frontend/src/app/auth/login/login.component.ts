import { Component, inject } from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {

  showPassword = false;
  esTecnico = false;

  // 🔒 Evita doble submit (doble clic / Enter + clic) mientras el login
  // está en curso: sin esto, cada clic dispara su propio POST /auth/login
  // y cada uno crea su propia fila en Sesiones Activas para el mismo
  // usuario, con segundos de diferencia.
  enviando = false;

  userAgent = navigator.userAgent;

  errorMessage = '';
  successMessage = '';

  email = '';
  password = '';

  //email = 'admin@admin.com';
  //password = 'Admin123*';

  //email = 'demo@empresademo.cl';
  //password = '123456789';

  loginData = {
    email: this.email,
    password: this.password,

    navegador: this.obtenerNavegador(),
    sistemaOperativo: this.obtenerSO(),
    dispositivo: this.obtenerDispositivo(),
    versionApp: '1.0.0'
  };

  authService = inject(AuthService)
  router = inject(Router);

  togglePassword() {
    this.showPassword = !this.showPassword;
  }

  login() {

    // 🔒 Si ya hay un login en curso, ignora el submit repetido.
    if (this.enviando) {
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';


    if (!this.loginData.email || !this.loginData.password) {
      this.errorMessage = 'Completa los campos';
      return;
    }

    this.enviando = true;

    this.authService.login(this.loginData).subscribe({
      next: async (res) => {

        this.successMessage = 'Inicio de sesión exitoso';

        this.authService.guardarToken(res.accessToken);
        this.authService.guardarRefreshToken(res.refreshToken);
        this.authService.setUserFromToken();
        this.authService.startRefreshTimer();

        this.esTecnico = this.authService.isTecnico()!;

        const destino = !this.esTecnico
          ? '/inicio/dashboard'
          : '/inicio/calendario';

        // 🔥 Si el servidor tiene una versión más nueva del frontend que
        // la que está corriendo en esta pestaña, esto navega de página
        // completa a `destino` (no una ruta del router de Angular), lo
        // que trae la versión nueva del frontend. El token ya quedó
        // guardado arriba, así que el usuario llega logueado.
        const recargando = await this.authService.verificarVersionYRecargarSiCorresponde(destino);

        if (recargando) {
          return;
        }

        // 🔒 enviando se mantiene en true hasta la redirección: evita
        // que un clic extra durante estos 800ms dispare otro login.
        setTimeout(() => {
          this.router.navigateByUrl(destino);
        }, 800);
      },
      error: (err) => {
        this.errorMessage = err.error?.error || 'Credenciales incorrectas';
        this.enviando = false;
      }
    });

  }

  obtenerNavegador(): string {

    const ua = navigator.userAgent;

    if (ua.includes('Edg')) return 'Edge';
    if (ua.includes('Chrome')) return 'Chrome';
    if (ua.includes('Firefox')) return 'Firefox';
    if (ua.includes('Safari')) return 'Safari';

    return 'Desconocido';
  }

  obtenerSO(): string {

    const ua = navigator.userAgent;

    if (ua.includes('Windows')) return 'Windows';
    if (ua.includes('Mac')) return 'MacOS';
    if (ua.includes('Linux')) return 'Linux';
    if (ua.includes('Android')) return 'Android';
    if (ua.includes('iPhone')) return 'iOS';

    return 'Desconocido';
  }

  obtenerDispositivo(): string {

    const ua = navigator.userAgent;

    const esMovil =
      /Android|iPhone|iPad|iPod/i.test(ua);

    return esMovil ? 'Móvil' : 'Escritorio';
  }

  goToForgotPassword() {
    this.router.navigateByUrl('/forgot-password');
  }

  invitado(){

    this.loginData = {
      email: 'demo@empresademo.cl',
      password: '123456789',

      navegador: this.obtenerNavegador(),
      sistemaOperativo: this.obtenerSO(),
      dispositivo: this.obtenerDispositivo(),
      versionApp: '1.0.0'
    };

    Swal.fire({
      icon: 'info',
      title: '¡Bienvenido!',
      html: `
        <p>
          Has ingresado como <b>Invitado</b>.
        </p>

        <div style="text-align:left; margin:15px 0;">
          ✔ Explora todas las funcionalidades.<br>
          ✔ Puedes crear y modificar información.<br>
          ⚠ Los datos de esta cuenta se reinician automáticamente cada día.
        </div>

        <input
          id="correoDemo"
          class="swal2-input"
          type="email"
          placeholder="Correo electrónico (opcional)">

        <div style="margin-top:10px;text-align:left;">
          <label style="display:flex;align-items:flex-start;gap:8px;">
            <input id="aceptaInfo" type="checkbox" style="margin-top:4px;">
            <span style="font-size:13px;">
              Al marcar esta opción, autorizo a recibir información sobre
              novedades, actualizaciones y promociones relacionadas con la plataforma.
            </span>
          </label>
        </div>
      `,
      confirmButtonText: 'Comenzar',
      allowOutsideClick: false,
      allowEscapeKey: false,
      focusConfirm: false,
      preConfirm: () => {

        const email = (document.getElementById('correoDemo') as HTMLInputElement).value.trim();
        const acepta = (document.getElementById('aceptaInfo') as HTMLInputElement).checked;

        if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
          Swal.showValidationMessage('Ingrese un correo electrónico válido.');
          return false;
        }

        return {
          email,
          acepta
        };
      }

    }).then((result) => {

      if (!result.isConfirmed) {
        return;
      }

      const { email, acepta } = result.value;

      // Si ingresó correo y aceptó recibir información
      if (email && acepta) {
        // Llamar a tu API para guardar el lead
        // this.demoService.registrarInteres(email).subscribe();
        this.login();
      } else {
        this.loginData = {
          email: '',
          password: '',

          navegador: '',
          sistemaOperativo: '',
          dispositivo: '',
          versionApp: ''
        };
      }


    });
  }

  clearError() {
    this.errorMessage = '';
  }

}
