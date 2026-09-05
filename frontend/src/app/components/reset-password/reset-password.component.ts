import { CommonModule } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './reset-password.component.html',
  styleUrl: './reset-password.component.css'
})
export class ResetPasswordComponent implements OnInit{

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private authService = inject(AuthService);

  token = '';
  password = '';
  confirmPassword = '';

  showPassword = false;
  loading = false;

  successMessage = '';
  errorMessage = '';

  ngOnInit() {
    this.token = this.route.snapshot.queryParamMap.get('token') || '';

    if (!this.token) {
      this.errorMessage = 'Token inválido';
      return;
    }

    // validar token
    this.authService.validateToken(this.token).subscribe({
      next: (res: any) => {
        console.log(res.message);
      },
      error: () => {
        this.errorMessage = 'Token inválido o expirado';
      }
    });
  }

  togglePassword() {
    this.showPassword = !this.showPassword;
  }

  resetPassword() {

    if (!this.password || !this.confirmPassword) {
      this.errorMessage = 'Completa los campos';
      return;
    }

    if (this.password !== this.confirmPassword) {
      this.errorMessage = 'Las contraseñas no coinciden';
      return;
    }

    this.loading = true;

    this.authService.resetPassword(this.token, this.password).subscribe({
      next: () => {
        this.loading = false;
        this.successMessage = 'Contraseña actualizada 🎉';
        this.errorMessage = '';

        setTimeout(() => {
          this.router.navigateByUrl('/login');
        }, 3000);
      },
      error: () => {
        this.loading = false;
        this.errorMessage = 'Error al cambiar contraseña';
      }
    });
  }

  goBack() {
    this.router.navigateByUrl('/login');
  }

}
