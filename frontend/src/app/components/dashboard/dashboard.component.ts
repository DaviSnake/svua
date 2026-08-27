import { Component, inject, OnInit } from '@angular/core';
import { DashboardService } from '../../services/dashboard.service';
import { CommonModule } from '@angular/common';
import { NgChartsModule } from 'ng2-charts';
import { SesionUsuarioService } from '../../services/sesion-usuario.service';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AuthService } from '../../services/auth.service';
import { EmpresaService } from '../../services/empresa.service';
import { Empresa } from '../../model/empresa';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, NgChartsModule, FormsModule, ReactiveFormsModule, MatAutocompleteModule, MatTooltipModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {

  data: any;
  kpis: any;

  doughnutData: any;
  doughnutOptions: any;
  lineData: any;
  lineOptions: any;
  barData: any;
  barOptions: any;
  rutaActual = '';

  dashboardService = inject(DashboardService);
  sesionUsuarioService = inject(SesionUsuarioService);
  router = inject(Router);
  private authService = inject(AuthService);
  private empresaService = inject(EmpresaService);

  // gráfico
  chartData: any;
  chartLabels = ['Operativos', 'Fuera de Servicio'];

  // 🔒 Filtro por empresa (mismo patrón de autocompletado que en
  // Reportes/Auditorías); solo visible/con efecto para SUPER_ADMIN.
  esSuperAdmin = false;
  empresas: Empresa[] = [];
  empresasFiltradas: Empresa[] = [];
  filtroEmpresaControl = new FormControl();
  filtroEmpresaId: number | null = null;

  ngOnInit(): void {
    this.esSuperAdmin = this.authService.isAdmin();

    if (this.esSuperAdmin) {
      this.cargarEmpresas();
      this.initFiltroEmpresa();
    }

    this.guardarActividad();
    this.cargarDashboard();
  }

  cargarDashboard(): void {
    this.dashboardService.getDashboard(this.filtroEmpresaId).subscribe(res => {
      this.data = res;
      this.initCharts();
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

  // 🔥 Solo recarga cuando se selecciona una empresa (objeto) o cuando
  // se borra el texto por completo (para volver a ver la propia).
  initFiltroEmpresa(): void {
    this.filtroEmpresaControl.valueChanges.subscribe(value => {
      const esObjeto = value && typeof value === 'object';
      const search = (esObjeto ? value.nombre : value || '').toLowerCase().trim();

      this.empresasFiltradas = !search
        ? this.empresas
        : this.empresas.filter(e => e.nombre.toLowerCase().includes(search));

      if (esObjeto) {
        this.filtroEmpresaId = value.id;
        this.cargarDashboard();
      } else if (!search && this.filtroEmpresaId !== null) {
        this.filtroEmpresaId = null;
        this.cargarDashboard();
      }
    });
  }

  initCharts() {

    // Doughnut (activos)
    this.doughnutData = {
      labels: ['Operativos', 'Fuera de Servicio', 'De Baja'],
      datasets: [{
        data: [
          this.data.activosOperativos,
          this.data.activosFueraServicio,
          this.data.activosDeBaja
        ]
      }]
    };

    // 🔥 Tooltip de la torta en porcentaje (ademas del valor absoluto):
    // el porcentaje se calcula sobre el total de LA TORTA (activos
    // Operativos + Fuera de Servicio + De Baja), no sobre el total de
    // la empresa (que podria incluir otros estados no graficados aca).
    this.doughnutOptions = {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        tooltip: {
          callbacks: {
            label: (context: any) => {
              const valor = context.parsed || 0;
              const total = (context.dataset.data as number[])
                .reduce((acc, v) => acc + (v || 0), 0);
              const porcentaje = total > 0 ? (valor / total) * 100 : 0;
              return `${context.label}: ${valor} (${porcentaje.toFixed(1)}%)`;
            }
          }
        }
      }
    };

    // Línea (depreciación)
    this.lineData = {
      labels: this.data.meses,
      datasets: [{
        label: 'Depreciación',
        data: this.data.depreciacionMensual
      }]
    };

    // 🔥 Antes habia que apuntar el mouse justo encima del punto para
    // que apareciera el tooltip (comportamiento por defecto de
    // Chart.js: intersect=true). Con interaction/hover en modo
    // 'index' + intersect=false, el tooltip aparece con solo acercar
    // el mouse a esa columna del eje X, sin tener que acertarle al
    // punto exacto. hitRadius agranda ademas el area invisible de cada
    // punto (el punto se sigue viendo del mismo tamaño, radius/
    // hoverRadius no cambian el dibujo).
    this.lineOptions = {
      responsive: true,
      maintainAspectRatio: false,
      interaction: {
        mode: 'index',
        intersect: false
      },
      hover: {
        mode: 'index',
        intersect: false
      },
      elements: {
        point: {
          hitRadius: 20
        }
      },
      plugins: {
        tooltip: {
          mode: 'index',
          intersect: false
        }
      }
    };

    // Barras (órdenes)
    this.barData = {
      labels: ['Pendientes', 'En Ejecucion', 'Pre Completadas', 'Completadas', 'Programadas', 'Canceladas'],
      datasets: [{
        label: 'Órdenes', // 🔥 AQUÍ ESTÁ LA CLAVE
        data: this.data.ordenesPorEstado
      }]
    };

    // 🔥 Mismo ajuste que en lineOptions: sin esto, Chart.js exige
    // apuntar justo encima de la barra (intersect=true por defecto)
    // para mostrar el tooltip. Con modo 'index' + intersect=false
    // alcanza con acercar el mouse a esa columna del eje X.
    this.barOptions = {
      responsive: true,
      maintainAspectRatio: false,
      interaction: {
        mode: 'index',
        intersect: false
      },
      hover: {
        mode: 'index',
        intersect: false
      },
      plugins: {
        tooltip: {
          mode: 'index',
          intersect: false
        }
      }
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

  guardarActividad() {
    this.router.events
      .pipe(
        filter(event => event instanceof NavigationEnd)
      )
      .subscribe((event: any) => {

        this.rutaActual = event.urlAfterRedirects;

        if (this.rutaActual != "/login" && this.rutaActual != "/forgot-password"){
          this.sesionUsuarioService.actualizarActividad(
            event.urlAfterRedirects,
            'Navegación'
          ).subscribe();
        }

      });
  }

}
