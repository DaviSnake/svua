import { Component, inject, OnInit } from '@angular/core';
import { DashboardResponse } from './models/reportes.model';
import { DashboardService } from '../../services/dashboard.service';
import { NgChartsModule } from 'ng2-charts';
import { ChartConfiguration, ChartType } from 'chart.js';

@Component({
  selector: 'app-reportes',
  standalone: true,
  imports: [NgChartsModule],
  templateUrl: './reportes.component.html',
  styleUrl: './reportes.component.css'
})
export class ReportesComponent implements OnInit {

  data?: DashboardResponse;

   dashboardService = inject(DashboardService);

   dataCostos: any;

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
    this.cargarCostos();
  }

  cargarDashboard() {
    this.dashboardService.getDashboardIndicadores().subscribe({
      next: (res) => {
        this.data = res;
      }
    });
  }

  cargarCostos(): void {
    this.dashboardService.getCostos().subscribe({
      next: (res) => {
        console.log(res);

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
