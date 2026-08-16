import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatTooltipModule } from '@angular/material/tooltip';
import { DashboardResponse } from './models/reportes.model';
import { DashboardService } from '../../services/dashboard.service';
import { ActivoService } from '../../services/activo.service';
import { EmpresaService } from '../../services/empresa.service';
import { AuthService } from '../../services/auth.service';
import { Activo } from '../../model/activo';
import { Empresa } from '../../model/empresa';
import { NgChartsModule } from 'ng2-charts';
import { ChartConfiguration, ChartType } from 'chart.js';

@Component({
  selector: 'app-reportes',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, MatAutocompleteModule, MatTooltipModule, NgChartsModule],
  templateUrl: './reportes.component.html',
  styleUrl: './reportes.component.css'
})
export class ReportesComponent implements OnInit {

  data?: DashboardResponse;

   dashboardService = inject(DashboardService);
   private activoService = inject(ActivoService);
   private empresaService = inject(EmpresaService);
   private authService = inject(AuthService);

   dataCostos: any;

  // 🔥 Filtro por activo (para todos los usuarios): acota la evolución
  // de costos de mantención a un solo activo. Mismo patrón de
  // autocompletado usado en los informes (Empresa).
  activos: Activo[] = [];
  activosFiltrados: Activo[] = [];
  filtroActivoControl = new FormControl();
  filtroActivoId: number | null = null;

  // 🔒 Filtro por empresa, solo visible/con efecto para SUPER_ADMIN:
  // acota tanto el monitoreo de mantenimiento (KPIs) como la evolución
  // de costos a una empresa puntual.
  esAdmin = false;
  empresas: Empresa[] = [];
  empresasFiltradas: Empresa[] = [];
  filtroEmpresaControl = new FormControl();
  filtroEmpresaId: number | null = null;

   public lineChartType: ChartType = 'line';

   public lineChartData: ChartConfiguration['data'] = {
    labels: [],
    datasets: [
      {
        data: [],
        label: 'Costos ($)',
        fill: true,
        tension: 0.4,
        borderColor: '#3f51b5',
        backgroundColor: 'rgba(63,81,181,0.2)'
      }
    ]
  };

  public lineChartOptions: ChartConfiguration['options'] = {
    responsive: true,
    maintainAspectRatio: false,

    plugins: {
      legend: {
        display: true
      }
    },

    scales: {
      y: {
        ticks: {
          callback: (value) =>
            '$ ' + new Intl.NumberFormat('es-CL').format(Number(value))
        }
      }
    }
  };


  ngOnInit(): void {
    this.esAdmin = this.authService.isAdmin();

    if (this.esAdmin) {
      this.cargarEmpresas();
      this.initFiltroEmpresa();
    }

    this.cargarDashboard();
    this.cargarActivos();
    this.initFiltroActivo();
    this.cargarCostos();
  }

  cargarDashboard() {
    this.dashboardService.getDashboardIndicadores(this.filtroEmpresaId).subscribe({
      next: (res) => {
        this.data = res;
      }
    });
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

  // 🔥 Igual que en los informes: solo recarga cuando se selecciona una
  // empresa (objeto) o cuando se borra el texto por completo. Al
  // cambiar la empresa se recargan los KPIs, el gráfico de costos y el
  // combo de Activo (acotado a la nueva empresa).
  initFiltroEmpresa(): void {
    this.filtroEmpresaControl.valueChanges.subscribe(value => {
      const esObjeto = value && typeof value === 'object';
      const search = (esObjeto ? value.nombre : value || '').toLowerCase().trim();

      this.empresasFiltradas = !search
        ? this.empresas
        : this.empresas.filter(e => e.nombre.toLowerCase().includes(search));

      if (esObjeto) {
        this.filtroEmpresaId = value.id;
        this.onEmpresaCambiada();
      } else if (!search && this.filtroEmpresaId !== null) {
        this.filtroEmpresaId = null;
        this.onEmpresaCambiada();
      }
    });
  }

  // 🔥 El activo seleccionado puede no pertenecer a la nueva empresa:
  // se limpia el filtro de Activo y se recarga su combo acotado a la
  // empresa elegida (o a todas, si se borró el filtro de empresa).
  private onEmpresaCambiada(): void {
    this.filtroActivoId = null;
    this.filtroActivoControl.setValue('', { emitEvent: false });

    this.cargarActivos();
    this.cargarDashboard();
    this.cargarCostos();
  }

  cargarActivos(): void {
    this.activoService.getActivoCombo(0, 200, this.filtroEmpresaId).subscribe(data => {
      this.activos = data.content;
      this.activosFiltrados = data.content;
    });
  }

  displayActivo = (activo: any): string => activo?.nombre ?? '';

  onFocusFiltroActivo(): void {
    this.activosFiltrados = this.activos;
  }

  // 🔥 Igual que los filtros de Empresa en los informes: solo recarga
  // cuando se selecciona un activo (objeto) o cuando se borra el texto
  // por completo.
  initFiltroActivo(): void {
    this.filtroActivoControl.valueChanges.subscribe(value => {
      const esObjeto = value && typeof value === 'object';
      const search = (esObjeto ? value.nombre : value || '').toLowerCase().trim();

      this.activosFiltrados = !search
        ? this.activos
        : this.activos.filter(a => a.nombre.toLowerCase().includes(search));

      if (esObjeto) {
        this.filtroActivoId = value.id;
        this.cargarCostos();
      } else if (!search && this.filtroActivoId !== null) {
        this.filtroActivoId = null;
        this.cargarCostos();
      }
    });
  }

  cargarCostos(): void {
    this.dashboardService.getCostos(this.filtroActivoId, this.filtroEmpresaId).subscribe({
      next: (res) => {
        this.setData(res);
      },
      error: (err) => {
        console.error(err);
      }
    });
  }
  setData(res: any): void {
    this.lineChartData = {
      labels: res.categorias,
      datasets: [
        {
          data: res.series,
          label: 'Costos ($)',
          fill: true,
          tension: 0.4,
          borderColor: '#3f51b5',
          backgroundColor: 'rgba(63,81,181,0.2)'
        }
      ]
    };
  }

}
