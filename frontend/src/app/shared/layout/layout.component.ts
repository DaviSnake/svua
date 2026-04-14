import { Component, inject } from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './layout.component.html',
  styleUrl: './layout.component.css'
})
export class LayoutComponent {

  authService = inject(AuthService)
  router = inject(Router);

  logout() {
    this.authService.logout().subscribe({
      next: () => {
        localStorage.removeItem('token'); // limpiar token
        localStorage.removeItem('refreshToken'); // limpiar refreshToken
        console.log("Logout exitoso");
        this.router.navigateByUrl('/login');
      },
      error: (err) => {
        console.error("Error en logout", err);
      }
    });
  }

}
