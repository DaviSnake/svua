import { Component, ElementRef, ViewChild } from '@angular/core';
import { HeaderComponent } from "./components/header/header.component";
import { SidebarComponent } from "./components/sidebar/sidebar.component";
import { RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [SidebarComponent, RouterOutlet, CommonModule],
  templateUrl: './layout.component.html',
  styleUrl: './layout.component.css'
})
export class LayoutComponent {

  @ViewChild('sidebarPadre') sidebarPadre!: ElementRef;

  sidebarOpen = false;

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
