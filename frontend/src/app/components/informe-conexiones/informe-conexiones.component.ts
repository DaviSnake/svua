import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { SesionUsuarioService } from '../../services/sesion-usuario.service';
import { EmpresaService } from '../../services/empresa.service';
import { Empresa } from '../../model/empresa';
import { SesionUsuarioResponse } from '../sesion-usuario/models/sesion-usuario.model';
import { calcularPaginasVisibles } from '../../shared/pagination.util';

@Component({
  selector: 'app-informe-conexiones',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, MatAutocompleteModule],
  templateUrl: './informe-conexiones.component.html',
  styleUrl: './informe-conexiones.component.css'
})
export class InformeConexionesComponent implements OnInit {

  private sesionUsuarioService = inject(SesionUsuarioService);
  private empresaService = inject(EmpresaService);

  conexiones: SesionUsuarioResponse[] = [];

  // 🔥 Filtros: usuario (texto libre) y fecha (input date) recargan al
  // escribir/seleccionar; empresa usa el mismo patrón de autocompletado
  // que el resto de los mantenedores/filtros de grilla.
  filtroUsuario = '';
  filtroFecha = '';

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
    this.cargarConexiones();
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

  // 🔥 Igual que en auditorías: solo recarga cuando se selecciona una
  // empresa (objeto) o cuando se borra el texto por completo.
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
        this.cargarConexiones();
      } else if (!search && this.filtroEmpresaId !== null) {
        this.filtroEmpresaId = null;
        this.page = 0;
        this.cargarConexiones();
      }
    });
  }

  onFiltroUsuarioChange(): void {
    this.page = 0;
    this.cargarConexiones();
  }

  onFiltroFechaChange(): void {
    this.page = 0;
    this.cargarConexiones();
  }

  cargarConexiones(): void {

    this.sesionUsuarioService
      .obtenerHistorialConexiones(
        this.page,
        this.size,
        this.filtroUsuario || undefined,
        this.filtroEmpresaId ?? undefined,
        this.filtroFecha || undefined
      )
      .subscribe({
        next: (data) => {
          this.conexiones = data.content;
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
    this.cargarConexiones();
  }

  // 🔥 Botones de página a mostrar (con "..." si hay muchas), en vez de
  // listar un botón por cada página.
  paginasVisibles(): number[] {
    return calcularPaginasVisibles(this.page, this.totalPages);
  }

  // 🔥 trackBy para la tabla de historial de conexiones.
  trackByConexionId(index: number, conexion: any): any {
    return conexion?.id ?? index;
  }

}
