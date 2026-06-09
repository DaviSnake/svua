import { Component, inject, OnInit } from '@angular/core';
import { HistorialActivoCompleto } from './models/historial-completo.model';
import { HistorialService } from '../../services/historial.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import * as XLSX from 'xlsx';
import { saveAs } from 'file-saver';

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

  activosExpandidos: Set<number> = new Set();

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

  toggleActivo(activoId: number): void {

    if (this.activosExpandidos.has(activoId)) {
      this.activosExpandidos.delete(activoId);
    } else {
      this.activosExpandidos.add(activoId);
    }

  }

  estaExpandido(activoId: number): boolean {
    return this.activosExpandidos.has(activoId);
  }

  exportarExcel(): void {

  const data: any[] = [];

  this.historialesFiltrados.forEach(activo => {

    activo.eventos.forEach(evento => {

      data.push({
        'ID Activo': activo.activoId,
        'Activo': activo.nombreActivo,
        'Valor Adquisición': activo.valorAdquisicion,
        'Valor Residual': activo.valorResidual,
        'Cantidad Mantenciones': activo.cantidadMantenciones,
        'Costo Mantenciones': activo.costoMantenciones,

        'Fecha Evento': evento.fecha,
        'Tipo Evento': evento.tipo,
        'Descripción': evento.descripcion,
        'Usuario': evento.usuario ?? '',
        'Proveedor': evento.proveedor ?? '',
        'Horas Trabajo': evento.horasTrabajo ?? '',
        'Valor Hora': evento.valorHora ?? '',
        'Costo Mano Obra': evento.costoManoObra ?? '',
        'Costo Total Evento': evento.costoTotal ?? ''
      });

    });

  });

  const worksheet = XLSX.utils.json_to_sheet(data);

  const workbook = XLSX.utils.book_new();

  XLSX.utils.book_append_sheet(
    workbook,
    worksheet,
    'Historial Activos'
  );

  const excelBuffer =
    XLSX.write(
      workbook,
      {
        bookType: 'xlsx',
        type: 'array'
      }
    );

  const blob = new Blob(
    [excelBuffer],
    {
      type:
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8'
    }
  );

  saveAs(
    blob,
    `historial_activos_${new Date().getTime()}.xlsx`
  );
}

}
