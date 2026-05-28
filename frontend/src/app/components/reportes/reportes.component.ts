import { Component, inject, OnInit } from '@angular/core';
import { DashboardResponse } from './models/reportes.model';
import { DashboardService } from '../../services/dashboard.service';

@Component({
  selector: 'app-reportes',
  standalone: true,
  imports: [],
  templateUrl: './reportes.component.html',
  styleUrl: './reportes.component.css'
})
export class ReportesComponent implements OnInit {

  data?: DashboardResponse;

   dashboardService = inject(DashboardService);

   dataCostos: any;


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

  cargarCostos() {
    this.dashboardService.getCostos().subscribe({
      next: (res) => {

        this.dataCostos = {
          series: [
            {
              name: 'Costos',
              dataCostos: res.series
            }
          ],

          chart: {
            type: 'bar',
            height: 350
          },

          xaxis: {
            categories: res.categorias
          }
        };

      }
    });
  }

}
