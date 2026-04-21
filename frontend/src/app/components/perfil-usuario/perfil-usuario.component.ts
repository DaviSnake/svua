import { CommonModule } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { UsuarioService } from '../../services/usuario.service';
import { PerfilUsuario } from '../../model/perfilUsuario';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-perfil-usuario',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './perfil-usuario.component.html',
  styleUrl: './perfil-usuario.component.css'
})
export class PerfilUsuarioComponent implements OnInit {

  usuarioService = inject(UsuarioService);
  perfil!: PerfilUsuario;

  ngOnInit(): void {
    this.cargarPerfilUsuarios();
  }

  cargarPerfilUsuarios() {
    this.usuarioService.getPerfilUsurio().subscribe(data => {
      this.perfil = data;
    });
  }

}
