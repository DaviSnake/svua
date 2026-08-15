import { Component, inject, OnInit } from '@angular/core';
import { HistorialActivoCompleto } from './models/historial-completo.model';
import { HistorialService } from '../../services/historial.service';
import { CommonModule } from '@angular/common';
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { AuthService } from '../../services/auth.service';
import { EmpresaService } from '../../services/empresa.service';
import { Empresa } from '../../model/empresa';
import * as XLSX from 'xlsx';
import { saveAs } from 'file-saver';
import { calcularPaginasVisibles } from '../../shared/pagination.util';

@Component({
  selector: 'app-auditorias',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, MatAutocompleteModule],
  templateUrl: './auditorias.component.html',
  styleUrl: './auditorias.component.css'
})
export class AuditoriasComponent implements OnInit {

  private historialService = inject(HistorialService);
  private authService = inject(AuthService);
  private empresaService = inject(EmpresaService);

  historiales: HistorialActivoCompleto[] = [];
  historialesFiltrados: HistorialActivoCompleto[] = [];
  historialesPaginados: HistorialActivoCompleto[] = [];

  activosExpandidos: Set<number> = new Set();

  textoBusqueda = '';

  esSuperAdmin = false;

  // 🔥 Filtro por empresa (mismo patrón de autocompletado que en los
  // mantenedores); solo tiene efecto real para SUPER_ADMIN, que es el
  // único rol que ve registros de más de una empresa a la vez.
  empresas: Empresa[] = [];
  empresasFiltroFiltradas: Empresa[] = [];
  filtroEmpresaControl = new FormControl();
  filtroEmpresaId: number | null = null;

  page = 0;
  size = 10;

  totalPages = 0;
  totalElements = 0;

  ngOnInit() {
    this.esSuperAdmin = this.authService.isAdmin();
    this.initFiltroEmpresa();
    this.cargarHistorial();
    this.cargarEmpresas();
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

    this.historialService.obtenerHistorialCompleto(this.filtroEmpresaId)
      .subscribe({
        next: (data) => {
          this.historiales = data;
          this.historialesFiltrados = [...data];
          this.page = 0;
          this.actualizarPaginacion();
        }
      });
  }

  cargarEmpresas() {
    this.empresaService.getAll().subscribe(data => {
      this.empresas = data;
      this.empresasFiltroFiltradas = data;
    });
  }

  displayEmpresa = (empresa: any): string => empresa?.nombre ?? '';

  onFocusFiltroEmpresa() {
    this.empresasFiltroFiltradas = this.empresas;
  }

  // 🔥 Filtro de la grilla por empresa. Solo recarga cuando se selecciona
  // una empresa (objeto) o cuando se borra el texto por completo (para
  // volver a ver todas); mientras se escribe, solo filtra las opciones
  // del desplegable.
  initFiltroEmpresa() {
    this.filtroEmpresaControl.valueChanges.subscribe(value => {
      const esObjeto = value && typeof value === 'object';
      const search = (esObjeto ? value.nombre : value || '').toLowerCase().trim();

      this.empresasFiltroFiltradas = !search
        ? this.empresas
        : this.empresas.filter(e => e.nombre.toLowerCase().includes(search));

      if (esObjeto) {
        this.filtroEmpresaId = value.id;
        this.cargarHistorial();
      } else if (!search && this.filtroEmpresaId !== null) {
        this.filtroEmpresaId = null;
        this.cargarHistorial();
      }
    });
  }

  filtrarActivos() {

    const texto = this.textoBusqueda
      .toLowerCase()
      .trim();

    if (!texto) {

      this.historialesFiltrados = [...this.historiales];
    } else {

      this.historialesFiltrados =
        this.historiales.filter(activo =>
          activo.nombreActivo.toLowerCase().includes(texto)
          || activo.activoId.toString().includes(texto)
        );
    }

    this.page = 0;
    this.actualizarPaginacion();
  }

  // 🔥 El historial completo ya viene cargado de una sola vez
  // (obtenerHistorialCompleto), así que la paginación es en el cliente:
  // se recorta el arreglo ya filtrado según la página actual.
  actualizarPaginacion(): void {

    this.totalElements = this.historialesFiltrados.length;
    this.totalPages = Math.max(1, Math.ceil(this.totalElements / this.size));

    const inicio = this.page * this.size;
    this.historialesPaginados =
      this.historialesFiltrados.slice(inicio, inicio + this.size);
  }

  cambiarPagina(p: number): void {

    if (p < 0 || p >= this.totalPages) {
      return;
    }

    this.page = p;
    this.actualizarPaginacion();
  }

  // 🔥 Botones de página a mostrar (con "..." si hay muchas), en vez de
  // listar un botón por cada página.
  paginasVisibles(): number[] {
    return calcularPaginasVisibles(this.page, this.totalPages);
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

          'Fecha Evento': new Date(evento.fecha)
            .toLocaleDateString('es-CL', {
              day: '2-digit',
              month: '2-digit',
              year: 'numeric'
            }),
          'Tipo Evento': evento.tipo,
          'Tipo Mantención': evento.tipoMantenimiento ?? '',
          'Descripción': evento.descripcion,
          'Usuario': evento.usuario ?? '',
          'Proveedor': evento.proveedor ?? '',
          'Horas Trabajo': evento.horasTrabajo ?? '',
          'Valor Hora': evento.valorHora ?? '',
          'Costo Mano Obra': evento.costoManoObra ?? '',
          'Costo Total Evento': evento.costoTotal ?? '',
          'Repuestos': evento.repuestos?.join(', ') ?? ''
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

  // 🔥 trackBy para la lista principal de activos con historial.
  trackByActivoId(index: number, activo: any): any {
    return activo?.activoId ?? index;
  }

}
