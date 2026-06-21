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

    const data = {
      email: this.email
    };

    this.authService.forgotPassword(data).subscribe({
      next: (res: any) => {
        this.message = res.message || 'Revisa tu correo 📩';
        this.errorMessage = '';

        this.message = 'Correo enviado 📩';

        setTimeout(() => {
          this.goBack();
        }, 3000);
      },
      error: (err) => {
        this.errorMessage = err.error?.error || 'Error al enviar correo';
        this.message = '';

        setTimeout(() => this.errorMessage = '', 4000);
      }
    });
  }

  goBack() {
    this.router.navigateByUrl('/login');
  }

}
