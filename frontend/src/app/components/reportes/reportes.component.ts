import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { DashboardResponse } from './models/reportes.model';
import { DashboardService } from '../../services/dashboard.service';
import { ActivoService } from '../../services/activo.service';
import { Activo } from '../../model/activo';
import { NgChartsModule } from 'ng2-charts';
import { ChartConfiguration, ChartType } from 'chart.js';

@Component({
  selector: 'app-reportes',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, MatAutocompleteModule, NgChartsModule],
  templateUrl: './reportes.component.html',
  styleUrl: './reportes.component.css'
})
export class ReportesComponent implements OnInit {

  data?: DashboardResponse;

   dashboardService = inject(DashboardService);
   private activoService = inject(ActivoService);

   dataCostos: any;

  // 🔥 Filtro por activo (para todos los usuarios): acota la evolución
  // de costos de mantención a un solo activo. Mismo patrón de
  // autocompletado usado en los informes (Empresa).
  activos: Activo[] = [];
  activosFiltrados: Activo[] = [];
  filtroActivoControl = new FormControl();
  filtroActivoId: number | null = null;

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
    this.cargarDashboard();
    this.cargarActivos();
    this.initFiltroActivo();
    this.cargarCostos();
  }

  cargarDashboard() {
    this.dashboardService.getDashboardIndicadores().subscribe({
      next: (res) => {
        this.data = res;
      }
    });
  }

  cargarActivos(): void {
    this.activoService.getActivoCombo().subscribe(data => {
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
    this.dashboardService.getCostos(this.filtroActivoId).subscribe({
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
