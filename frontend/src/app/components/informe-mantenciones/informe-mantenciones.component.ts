import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import Swal from 'sweetalert2';
import { OrdenMantencionService } from '../../services/orden-mantencion.service';
import { EmpresaService } from '../../services/empresa.service';
import { Empresa } from '../../model/empresa';
import { OrdenMantenimientoReporte } from '../../model/ordenMantenimientoReporte';
import { calcularPaginasVisibles } from '../../shared/pagination.util';

@Component({
  selector: 'app-informe-mantenciones',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, MatAutocompleteModule],
  templateUrl: './informe-mantenciones.component.html',
  styleUrl: './informe-mantenciones.component.css'
})
export class InformeMantencionesComponent implements OnInit {

  private ordenMantencionService = inject(OrdenMantencionService);
  private empresaService = inject(EmpresaService);

  ordenes: OrdenMantenimientoReporte[] = [];

  // 🔥 Mismos filtros que el informe de conexiones: usuario (texto
  // libre), fecha (input date) y empresa (autocompletado); además el
  // filtro por estado, propio de este informe.
  filtroUsuario = '';
  filtroFecha = '';

  // 🔥 Filtro por estado: parte en "Completada" para que el informe
  // siga funcionando por defecto como comprobante de trabajos ya
  // ejecutados; el usuario puede elegir otro estado o "Todos" (valor
  // vacío = sin filtro).
  filtroEstado = 'COMPLETADA';
  estadosDisponibles = [
    { value: '', label: 'Todos' },
    { value: 'PENDIENTE', label: 'Pendiente' },
    { value: 'PROGRAMADA', label: 'Programada' },
    { value: 'EN_EJECUCION', label: 'En Ejecución' },
    { value: 'PRE_COMPLETADA', label: 'Pre Completada' },
    { value: 'COMPLETADA', label: 'Completada' },
    { value: 'CANCELADA', label: 'Cancelada' },
    { value: 'ATRASADA', label: 'Atrasada' }
  ];

  empresas: Empresa[] = [];
  empresasFiltradas: Empresa[] = [];
  filtroEmpresaControl = new FormControl();
  filtroEmpresaId: number | null = null;

  page = 0;
  size = 10;

  totalPages = 0;
  totalElements = 0;

  // 🔥 Id de la orden cuyo comprobante está expandido (null = ninguna).
  ordenExpandidaId: number | null = null;

  ngOnInit(): void {
    this.initFiltroEmpresa();
    this.cargarEmpresas();
    this.cargarOrdenes();
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

  // 🔥 Igual que en informe de conexiones: solo recarga cuando se
  // selecciona una empresa (objeto) o cuando se borra el texto por
  // completo.
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
        this.cargarOrdenes();
      } else if (!search && this.filtroEmpresaId !== null) {
        this.filtroEmpresaId = null;
        this.page = 0;
        this.cargarOrdenes();
      }
    });
  }

  onFiltroUsuarioChange(): void {
    this.page = 0;
    this.cargarOrdenes();
  }

  onFiltroFechaChange(): void {
    this.page = 0;
    this.cargarOrdenes();
  }

  onFiltroEstadoChange(): void {
    this.page = 0;
    this.cargarOrdenes();
  }

  cargarOrdenes(): void {

    this.ordenMantencionService
      .obtenerInformeMantenciones(
        this.page,
        this.size,
        this.filtroUsuario || undefined,
        this.filtroEmpresaId ?? undefined,
        this.filtroEstado || undefined,
        this.filtroFecha || undefined
      )
      .subscribe({
        next: (data) => {
          this.ordenes = data.content;
          this.page = data.page.number;
          this.totalPages = data.page.totalPages;
          this.totalElements = data.page.totalElements;
          this.ordenExpandidaId = null;
        }
      });
  }

  cambiarPagina(p: number): void {

    if (p < 0 || p >= this.totalPages) {
      return;
    }

    this.page = p;
    this.cargarOrdenes();
  }

  // 🔥 Botones de página a mostrar (con "..." si hay muchas), en vez de
  // listar un botón por cada página.
  paginasVisibles(): number[] {
    return calcularPaginasVisibles(this.page, this.totalPages);
  }

  // 🔥 Muestra/oculta el comprobante detallado de una orden (repuestos,
  // valores, etc.), con el mismo formato de referencia entregado por el
  // usuario ("Orden #5 - Prueba 1 (COMPLETADA)...").
  toggleDetalle(id: number | undefined): void {

    if (id == null) {
      return;
    }

    this.ordenExpandidaId = this.ordenExpandidaId === id ? null : id;
  }

  estaExpandida(id: number | undefined): boolean {
    return id != null && this.ordenExpandidaId === id;
  }

  // 🔥 Duración de la mantención expresada en horas (redondeada a 2
  // decimales), a partir de duracionSegundos que entrega el backend.
  formatearDuracion(duracionSegundos: number | undefined): string {

    if (duracionSegundos == null || duracionSegundos <= 0) {
      return '—';
    }

    const horas = Math.round((duracionSegundos / 3600) * 100) / 100;

    return `${horas} h`;
  }

  // 🔥 trackBy para la tabla de órdenes del informe y el detalle de
  // repuestos utilizados de cada orden expandida.
  trackByOrdenId(index: number, orden: any): any {
    return orden?.id ?? index;
  }

  trackByRepuestoId(index: number, repuesto: any): any {
    return repuesto?.id ?? repuesto?.repuestoId ?? index;
  }

  // 🔥 Igual patrón que "Ver Logs": se abre la pestaña de forma
  // sincrónica dentro del click (para que el navegador no la bloquee
  // como popup) y una vez llega el archivo se navega esa misma pestaña
  // al blob. No se llama si la orden no tiene checklist (ver
  // orden.tieneChecklist en el template).
  verChecklist(ordenId: number | undefined): void {

    if (ordenId == null) {
      return;
    }

    const nuevaPestana = window.open('', '_blank');

    this.ordenMantencionService.verArchivo(ordenId).subscribe({
      next: (blob) => {
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
          title: err.error?.error || 'No fue posible cargar el checklist'
        });
      }
    });
  }

}
