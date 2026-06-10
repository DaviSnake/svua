import { Component, inject } from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {

  showPassword = false;

  userAgent = navigator.userAgent;

  errorMessage = '';
  successMessage = '';

  email = '';
  password = '';

  //email = 'admin@admin.com';
  //password = 'Admin123*';

  //email = 'admin@empresademo.cl';
  //password = 'Admin12345';

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

    this.errorMessage = '';
    this.successMessage = '';

    if (!this.loginData.email || !this.loginData.password) {
      this.errorMessage = 'Completa los campos';
      return;
    }

    this.authService.login(this.loginData).subscribe({
      next: (res) => {

        this.successMessage = 'Inicio de sesión exitoso';

        this.authService.guardarToken(res.accessToken);
        this.authService.guardarRefreshToken(res.refreshToken);
        this.authService.setUserFromToken();
        this.authService.startRefreshTimer();

        setTimeout(() => {
          this.router.navigateByUrl('/inicio/dashboard');
        }, 800);
      },
      error: (err) => {
        this.errorMessage = err.error?.error || 'Credenciales incorrectas';
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

  clearError() {
    this.errorMessage = '';
  }

}
