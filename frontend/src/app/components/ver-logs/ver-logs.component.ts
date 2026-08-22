import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import Swal from 'sweetalert2';
import { LogArchivoService } from '../../services/log-archivo.service';
import { EmpresaService } from '../../services/empresa.service';
import { Empresa } from '../../model/empresa';
import { LogArchivoResponse } from './models/log-archivo.model';
import { calcularPaginasVisibles } from '../../shared/pagination.util';

// 🔥 Pantalla "Ver logs" (solo SUPER_ADMIN): grilla filtrable por
// empresa de los .txt de error generados por las cargas masivas (uno
// por ejecución con al menos un error, ver ImportFileLogService), con
// un botón "Ver" por fila que muestra el contenido del archivo.
@Component({
  selector: 'app-ver-logs',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, MatAutocompleteModule],
  templateUrl: './ver-logs.component.html',
  styleUrl: './ver-logs.component.css'
})
export class VerLogsComponent implements OnInit {

  private logArchivoService = inject(LogArchivoService);
  private empresaService = inject(EmpresaService);

  logs: LogArchivoResponse[] = [];

  empresas: Empresa[] = [];
  empresasFiltradas: Empresa[] = [];
  filtroEmpresaControl = new FormControl();
  filtroEmpresaId: number | null = null;

  page = 0;
  size = 10;

  totalPages = 0;
  totalElements = 0;

  ngOnInit(): void {
    this.initFiltroEmpresa();
    this.cargarEmpresas();
    this.cargarLogs();
  }

  cargarEmpresas(): void {
    this.empresaService.getAll().subscribe(data => {
      this.empresas = data;
      this.empresasFiltradas = data;
    });
  }

  displayEmpresa = (empresa: any): string => empresa?.nombre ?? '';

  onFocusFiltroEmpresa(): void {
    this.empresasFiltradas = this.empresas;
  }

  // 🔥 Igual patrón que Informe de Conexiones: solo recarga la grilla
  // cuando se selecciona una empresa (objeto) o se borra el texto.
  initFiltroEmpresa(): void {
    this.filtroEmpresaControl.valueChanges.subscribe(value => {
      const esObjeto = value && typeof value === 'object';
      const search = (esObjeto ? value.nombre : value || '').toLowerCase().trim();

      this.empresasFiltradas = !search
        ? this.empresas
        : this.empresas.filter(e => e.nombre.toLowerCase().includes(search));

      if (esObjeto) {
        this.filtroEmpresaId = value.id;
        this.page = 0;
        this.cargarLogs();
      } else if (!search && this.filtroEmpresaId !== null) {
        this.filtroEmpresaId = null;
        this.page = 0;
        this.cargarLogs();
      }
    });
  }

  cargarLogs(): void {

    this.logArchivoService
      .listar(this.page, this.size, this.filtroEmpresaId ?? undefined)
      .subscribe({
        next: (data) => {
          this.logs = data.content;
          this.page = data.page.number;
          this.totalPages = data.page.totalPages;
          this.totalElements = data.page.totalElements;
        }
      });
  }

  cambiarPagina(p: number): void {

    if (p < 0 || p >= this.totalPages) {
      return;
    }

    this.page = p;
    this.cargarLogs();
  }

  paginasVisibles(): number[] {
    return calcularPaginasVisibles(this.page, this.totalPages);
  }

  trackByLog(index: number, log: LogArchivoResponse): any {
    return `${log.empresaId}_${log.nombreArchivo}`;
  }

  // 🔥 Igual patrón que el visor de checklist adjunto de las órdenes:
  // se abre la pestaña en forma sincrónica dentro del click (para que
  // el navegador no la bloquee como popup) y una vez llega el
  // contenido se navega esa misma pestaña al blob de texto.
  verLog(log: LogArchivoResponse): void {

    const nuevaPestana = window.open('', '_blank');

    this.logArchivoService
      .verArchivo(log.empresaId, log.nombreArchivo)
      .subscribe({
        next: (contenido) => {

          const blob = new Blob([contenido], { type: 'text/plain;charset=utf-8' });
          const url = URL.createObjectURL(blob);

          if (nuevaPestana) {
            nuevaPestana.location.href = url;
          } else {
            window.open(url, '_blank');
          }
        },
        error: (err) => {

          if (nuevaPestana) {
            nuevaPestana.close();
          }

          Swal.fire({
            icon: 'error',
            title: err.error?.error || 'No fue posible cargar el archivo de log'
          });
        }
      });
  }

  formatearTamanio(bytes: number): string {

    if (!bytes) {
      return '0 KB';
    }

    const kb = bytes / 1024;

    return kb < 1024
      ? `${kb.toFixed(1)} KB`
      : `${(kb / 1024).toFixed(1)} MB`;
  }

}
