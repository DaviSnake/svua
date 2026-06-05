import { Component, inject, OnInit } from '@angular/core';
import { HistorialActivoCompleto } from './models/historial-completo.model';
import { HistorialService } from '../../services/historial.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-auditorias',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './auditorias.component.html',
  styleUrl: './auditorias.component.css'
})
export class AuditoriasComponent implements OnInit {

  private historialService = inject(HistorialService);

  historiales: HistorialActivoCompleto[] = [];
  historialesFiltrados: HistorialActivoCompleto[] = [];

  textoBusqueda = '';

  page = 0;
  size = 10;

  totalPages = 0;
  totalElements = 0;

  ngOnInit() {
    this.cargarHistorial();
  }

  cargarHistorialPage() {

    this.historialService.getAll()
      .subscribe({
        next: (data) => {
          this.historiales = data.content;

          this.page = data.page.number;
          this.totalPages = data.page.totalPages;
          this.totalElements = data.page.totalElements;
        }
      });
  }

  cargarHistorial() {

    this.historialService.obtenerHistorialCompleto()
      .subscribe({
        next: (data) => {
          this.historiales = data;
          this.historialesFiltrados = [...data];
        }
      });
  }

  filtrarActivos() {

    const texto = this.textoBusqueda
      .toLowerCase()
      .trim();

    if (!texto) {

      this.historialesFiltrados = [...this.historiales];
      return;
    }

    this.historialesFiltrados =
      this.historiales.filter(activo =>
        activo.nombreActivo.toLowerCase().includes(texto)
        || activo.activoId.toString().includes(texto)
      );
  }

}
