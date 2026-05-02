import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './forgot-password.component.html',
  styleUrl: './forgot-password.component.css'
})
export class ForgotPasswordComponent {

  email = '';
  loading = false;
  message = '';
  errorMessage = '';

  authService = inject(AuthService);
  router = inject(Router);

  sendRecovery() {

    if (!this.email) {
      this.errorMessage = 'Ingresa tu correo';
      return;
    }

    this.authService.forgotPassword(this.email).subscribe({
      next: () => {
        this.errorMessage = 'Revisa tu correo 📩';
      },
      error: () => {
        this.errorMessage = 'Error al enviar el correo';
      }
    });
  }

  goBack() {
    this.router.navigateByUrl('/login');
  }

}
