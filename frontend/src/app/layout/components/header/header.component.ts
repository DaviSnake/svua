import { Component, HostListener, inject, OnInit } from '@angular/core';
import { AuthService } from '../../../services/auth.service';
import { Router } from '@angular/router';
import { jwtDecode } from 'jwt-decode';
import { SidebarService } from '../../../services/sidebar.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [],
  templateUrl: './header.component.html',
  styleUrl: './header.component.css'
})
export class HeaderComponent implements OnInit {

  authService = inject(AuthService);
  sidebarService = inject(SidebarService);
  router = inject(Router);
  usuario: any;
  menuOpen = false;

  ngOnInit() {
      this.authService.user$.subscribe(user => {
      this.usuario = user;
    });
  }

  toggleSidebar() {
    this.sidebarService.toggle();
  }

  toggleMenu() {
    this.menuOpen = !this.menuOpen;
  }

  logout() {
    this.authService.logout().subscribe(() => {
      localStorage.clear();
      this.router.navigate(['/login']);
    });
  }

  irPerfil() {
    this.router.navigate(['/perfil']);
  }

  @HostListener('document:click', ['$event'])
  onClickOutside(event: any) {
    if (!event.target.closest('.user-menu')) {
      this.menuOpen = false;
    }
  }

}
