import { Component, inject, OnInit } from '@angular/core';
import { SesionUsuarioResponse } from './models/sesion-usuario.model';
import { CommonModule } from '@angular/common';
import { SesionUsuarioService } from '../../services/sesion-usuario.service';

@Component({
  selector: 'app-sesion-usuario',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './sesion-usuario.component.html',
  styleUrl: './sesion-usuario.component.css'
})
export class SesionUsuarioComponent implements OnInit {

  sesionUsuarioService = inject(SesionUsuarioService);

  sesiones: SesionUsuarioResponse[] = [];

  ngOnInit(): void {
    this.cargarSesiones();
  }

  cargarSesiones(): void {

    this.sesionUsuarioService
      .obtenerSesionesActivas()
      .subscribe({
        next: data => {
          this.sesiones = data;
        }
      });

  }

}
