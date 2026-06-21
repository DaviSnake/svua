import { Component, ElementRef, inject, OnInit, ViewChild } from '@angular/core';
import { HeaderComponent } from "./components/header/header.component";
import { SidebarComponent } from "./components/sidebar/sidebar.component";
import { Router, RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { NotificacionStateService } from '../services/notificacion-state.service';
import { AuthService } from '../services/auth.service';
import { NotificacionService } from '../services/notificacion.service';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [SidebarComponent, RouterOutlet, CommonModule],
  templateUrl: './layout.component.html',
  styleUrl: './layout.component.css'
})
export class LayoutComponent implements OnInit{

  notificacionState = inject(NotificacionStateService);
  authService = inject(AuthService);
  notificacionService = inject(NotificacionService);
  router = inject(Router);

  @ViewChild('sidebarPadre') sidebarPadre!: ElementRef;

  sidebarOpen = false;

  empresaId: number = 0;
  cantidadNoLeidas = 0;

  ngOnInit(){

    this.empresaId = this.authService.getEmpresaId() ?? 0;

    this.notificacionState.notificarActualizacion();

  }

  abrirNotificaciones() {

      this.router.navigateByUrl('/inicio/notificaciones');

  }

  toggleSidebar() {
    this.sidebarOpen = !this.sidebarOpen;
  }

  closeSidebar() {
    this.sidebarOpen = false;
  }
  
  menuBtnClick(flag: string): void {
    if (flag !== "1") {
      this.sidebarPadre.nativeElement.classList.remove('minimize');
    } else {
      this.sidebarPadre.nativeElement.classList.add('minimize');

    }
  }

}
