import { Component, inject, OnInit } from '@angular/core';
import { DashboardService } from '../../services/dashboard.service';
import { CommonModule } from '@angular/common';
import { NgChartsModule } from 'ng2-charts';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, NgChartsModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {

  data: any;
  kpis: any;

  doughnutData: any;
  lineData: any;
  barData: any;

  dashboardService = inject(DashboardService);

  // gráfico
  chartData: any;
  chartLabels = ['Operativos', 'Fuera de Servicio'];

  ngOnInit(): void {
    this.dashboardService.getDashboard().subscribe(res => {
      this.data = res;

      this.initCharts();
    });
  }

  initCharts() {

    // Doughnut (activos)
    this.doughnutData = {
      labels: ['Operativos', 'Fuera de Servicio'],
      datasets: [{
        data: [
          this.data.activosOperativos,
          this.data.activosFueraServicio
        ]
      }]
    };

    // Línea (depreciación)
    this.lineData = {
      labels: this.data.meses,
      datasets: [{
        label: 'Depreciación',
        data: this.data.depreciacionMensual
      }]
    };

    // Barras (órdenes)
    this.barData = {
      labels: ['Pendientes', 'En Ejecucion', 'Completadas', 'Programadas', 'Canceladas'],
      datasets: [{
        label: 'Órdenes', // 🔥 AQUÍ ESTÁ LA CLAVE
        data: this.data.ordenesPorEstado
      }]
    };
  }

  // 🔥 KPI dinámico
  getEstadoCritico(valor: number) {
    if (valor > 10) return 'critico';
    if (valor > 5) return 'medio';
    return 'ok';
  }

  cargarKPIs() {
    this.dashboardService.getResumen().subscribe(data => {
      this.kpis = data;

      this.chartData = {
        datasets: [
          {
            data: [
              data.activosOperativos,
              data.activosFueraServicio
            ]
          }
        ]
      };
    });
  }

}
