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
        console.log("Logout exitoso");
        this.completarLogout();
      },
      error: (err) => {
        console.error("Error en logout", err);
        // aunque falle el aviso al backend, igual cerramos la sesión local
        // para no dejar al usuario "atascado" en la pantalla actual
        this.completarLogout();
      }
    });
  }

  private completarLogout() {
    this.authService.finalizarSesionLocal();
    this.router.navigateByUrl('/login');
  }

}
