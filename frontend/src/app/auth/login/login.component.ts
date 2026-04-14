import { Component, inject } from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {

  showPassword = false;

  loginData = {
    email: 'admin@admin.com',
    password: 'Admin123*'
  };

  email = 'admin@admin.com';
  password = 'Admin123*';

  authService = inject(AuthService)
  router = inject(Router);

  togglePassword() {
    this.showPassword = !this.showPassword;
  }

  login() {

    if (!this.loginData.email || !this.loginData.password) {
      alert('Completa los campos');
      return;
    }

    this.authService.login(this.loginData).subscribe({
      next: (res) => {
        this.authService.guardarToken(res.accessToken);
        this.authService.guardarRefreshToken(res.refreshToken);
        this.authService.setUserFromToken(); // 🔥
        this.authService.startRefreshTimer();
        this.router.navigateByUrl('/inicio/dashboard');
      },
      error: (err) => {
        console.log(err.error); // 👈 DEBUG
        alert(err.error.error);
      }
    });
  }

}
