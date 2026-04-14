import { Component, ElementRef, EventEmitter, inject, OnInit, Output, ViewChild } from '@angular/core';
import { Router, RouterLink, RouterModule, RouterOutlet } from '@angular/router';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterModule],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css'
})
export class SidebarComponent implements OnInit {

  @ViewChild('sidebar') sidebar!: ElementRef;
  @ViewChild('menuBtn') menuBtn!: ElementRef;
  @ViewChild('sidebarBtn') sidebarBtn!: ElementRef;
  @ViewChild('menuItemDropdownGestion') menuItemDropdownGestion!: ElementRef;
  @ViewChild('menuItemDropdownOrganizacion') menuItemDropdownOrganizacion!: ElementRef;
  @ViewChild('menuItemDropdownAnalisis') menuItemDropdownAnalisis!: ElementRef;
  @ViewChild('subMenuGestion') subMenuGestion!: ElementRef;
  @ViewChild('subMenuOrganizacion') subMenuOrganizacion!: ElementRef;
  @ViewChild('subMenuAnalisis') subMenuAnalisis!: ElementRef;
  @ViewChild('salirBtn') salirBtn!: ElementRef;

  @Output() datoEmitido = new EventEmitter<string>();

  authService = inject(AuthService);
  router = inject(Router);
  usuario: any;

  esAdmin = false;
  esAdminEmpresa = false;
  esUser = false;
  
  ngOnInit() {
      this.authService.user$.subscribe(user => {
      this.usuario = user;

      this.esAdmin = this.authService.isAdmin();
      this.esAdminEmpresa = this.authService.isAdminEmpresa();
    });
  }

  menuBtnClick(): void {
    let mensaje = "0";
    if (this.sidebar.nativeElement.classList.contains('minimize')) {
      this.sidebar.nativeElement.classList.remove('minimize');
    } else {
      this.sidebar.nativeElement.classList.add('minimize');
      mensaje = "1";
    }
    this.datoEmitido.emit(mensaje);
  }

  toggleBodySidebar() {
    let mensaje = "0";
    if (this.sidebar.nativeElement.classList.contains('sidebar-hidden')) {
      this.sidebar.nativeElement.classList.remove('sidebar-hidden');
    } else {
      this.sidebar.nativeElement.classList.add('sidebar-hidden');
    }

    if (this.sidebarBtn.nativeElement.classList.contains('sidebar-btn-hidden')) {
      this.sidebarBtn.nativeElement.classList.remove('sidebar-btn-hidden');
    } else {
      this.sidebarBtn.nativeElement.classList.add('sidebar-btn-hidden');
      mensaje = "1";
    }
    this.datoEmitido.emit(mensaje);
  }

  activarSubMenuGestion(){
    if (!this.menuItemDropdownGestion.nativeElement.classList.contains('sub-menu-toggle')) {
      this.subMenuGestion.nativeElement.style.height = `${this.subMenuGestion.nativeElement.scrollHeight + 6}px`;
      this.subMenuGestion.nativeElement.style.padding = '0.2rem 0';
      this.menuItemDropdownGestion.nativeElement.classList.add('sub-menu-toggle');
    }else{
      this.subMenuGestion.nativeElement.style.height = '0';
      this.subMenuGestion.nativeElement.style.padding = '0';
      this.menuItemDropdownGestion.nativeElement.classList.remove('sub-menu-toggle');
    }
  }

  activarSubMenuOrganizacion(){
    if (!this.menuItemDropdownOrganizacion.nativeElement.classList.contains('sub-menu-toggle')) {
      this.subMenuOrganizacion.nativeElement.style.height = `${this.subMenuOrganizacion.nativeElement.scrollHeight + 6}px`;
      this.subMenuOrganizacion.nativeElement.style.padding = '0.2rem 0';
      this.menuItemDropdownOrganizacion.nativeElement.classList.add('sub-menu-toggle');
    }else{
      this.subMenuOrganizacion.nativeElement.style.height = '0';
      this.subMenuOrganizacion.nativeElement.style.padding = '0';
      this.menuItemDropdownOrganizacion.nativeElement.classList.remove('sub-menu-toggle');
    }
  }

  activarSubMenuAnalisis(){
    if (!this.menuItemDropdownAnalisis.nativeElement.classList.contains('sub-menu-toggle')) {
      this.subMenuAnalisis.nativeElement.style.height = `${this.subMenuAnalisis.nativeElement.scrollHeight + 6}px`;
      this.subMenuAnalisis.nativeElement.style.padding = '0.2rem 0';
      this.menuItemDropdownAnalisis.nativeElement.classList.add('sub-menu-toggle');
    }else{
      this.subMenuAnalisis.nativeElement.style.height = '0';
      this.subMenuAnalisis.nativeElement.style.padding = '0';
      this.menuItemDropdownAnalisis.nativeElement.classList.remove('sub-menu-toggle');
    }
  }

  onMenuEnter(){
    if (!this.sidebar.nativeElement.classList.contains('minimize')) return;
    if (this.menuItemDropdownGestion.nativeElement.classList.contains('sub-menu-toggle')) {
      this.subMenuGestion.nativeElement.style.height = '0';
      this.subMenuGestion.nativeElement.style.padding = '0';
      this.menuItemDropdownGestion.nativeElement.classList.remove('sub-menu-toggle');
    }
  }

  logout() {
    this.authService.logout().subscribe(() => {
      localStorage.clear();
      this.router.navigate(['/login']);
    });
  }

  enviar(data: string) {
    //this.productService.sendData(data);
  }

}
