import { Component, ElementRef, ViewChild } from '@angular/core';
import { HeaderComponent } from "./components/header/header.component";
import { SidebarComponent } from "./components/sidebar/sidebar.component";
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [SidebarComponent, RouterOutlet],
  templateUrl: './layout.component.html',
  styleUrl: './layout.component.css'
})
export class LayoutComponent {

  @ViewChild('sidebarPadre') sidebarPadre!: ElementRef;
  mensaje = '';
  
  recibirDato(dato: string) {
    this.mensaje = dato;
    this.menuBtnClick(this.mensaje);
  }
  
  menuBtnClick(flag: string): void {
    if (flag !== "1") {
      this.sidebarPadre.nativeElement.classList.remove('minimize');
    } else {
      this.sidebarPadre.nativeElement.classList.add('minimize');

    }
  }

}
