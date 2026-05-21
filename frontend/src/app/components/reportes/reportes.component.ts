import { Component, inject, OnInit } from '@angular/core';
import { PaginaencontruccionComponent } from "../paginaencontruccion/paginaencontruccion.component";
import { DashboardResponse } from './models/reportes.model';
import { DashboardService } from '../../services/dashboard.service';

@Component({
  selector: 'app-reportes',
  standalone: true,
  imports: [PaginaencontruccionComponent],
  templateUrl: './reportes.component.html',
  styleUrl: './reportes.component.css'
})
export class ReportesComponent implements OnInit {

  data?: DashboardResponse;

   dashboardService = inject(DashboardService);

   ngOnInit(): void {
    this.cargarDashboard();
  }

  cargarDashboard() {
    this.dashboardService.getDashboardIndicadores().subscribe({
      next: (res) => {
        this.data = res;
      }
    });
  }

}
