import { Component, OnInit, inject } from '@angular/core';
import { Router, RouterLink, RouterModule, NavigationEnd } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { CommonModule } from '@angular/common';
import { filter } from 'rxjs/operators';

type MenuKey = 'gestion' | 'organizacion' | 'analisis';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterModule, CommonModule],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css'
})
export class SidebarComponent implements OnInit {

  authService = inject(AuthService);
  router = inject(Router);

  usuario: any;

  // 🔥 estado sidebar
  isCollapsed = false;

  // 🔥 estado menús
  openMenus: Record<MenuKey, boolean> = {
    gestion: false,
    organizacion: false,
    analisis: false
  };

  // roles
  esAdmin = false;
  esAdminEmpresa = false;

  ngOnInit() {

    // usuario
    this.authService.user$.subscribe(user => {
      this.usuario = user;
      this.esAdmin = this.authService.isAdmin();
      this.esAdminEmpresa = this.authService.isAdminEmpresa();
    });

    // 🔥 abrir menú según ruta
    this.detectarRuta(this.router.url);

    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe((event: any) => {
        this.detectarRuta(event.url);
      });
  }

  // 🔥 detectar menú activo por URL
  detectarRuta(url: string) {

    this.openMenus = {
      gestion: false,
      organizacion: false,
      analisis: false
    };

    if (
      url.includes('/activo') ||
      url.includes('/tipoActivo') ||
      url.includes('/repuesto')
    ) {
      this.openMenus.gestion = true;
    }

    if (
      url.includes('/empresa') ||
      url.includes('/ubicacion') ||
      url.includes('/bodega') ||
      url.includes('/proveedor')
    ) {
      this.openMenus.organizacion = true;
    }

    if (
      url.includes('/analisis') ||
      url.includes('/reportes')
    ) {
      this.openMenus.analisis = true;
    }
  }

  // 🔒 cerrar todo
  closeAllMenus() {
    this.openMenus = {
      gestion: false,
      organizacion: false,
      analisis: false
    };
  }

  // 🔥 toggle menú (BLOQUEADO si está colapsado)
  toggleMenu(menu: MenuKey) {

    if (this.isCollapsed) return; // 🔥 CLAVE

    Object.keys(this.openMenus).forEach((key) => {
      const k = key as MenuKey;
      this.openMenus[k] = k === menu ? !this.openMenus[k] : false;
    });
  }

  // 🔥 toggle sidebar
  toggleSidebar() {
    this.isCollapsed = !this.isCollapsed;
  }

  logout() {
    this.authService.logout().subscribe(() => {
      sessionStorage.clear();
      this.router.navigate(['/login']);
    });
  }
}