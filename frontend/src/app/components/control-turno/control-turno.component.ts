import { CommonModule } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { NgChartsModule } from 'ng2-charts';
import { ChartType } from 'chart.js';
import ChartDataLabels from 'chartjs-plugin-datalabels';
import Swal from 'sweetalert2';

import { ControlTurnoService } from '../../services/control-turno.service';
import { AuthService } from '../../services/auth.service';
import { EmpresaService } from '../../services/empresa.service';
import { PuntoControl } from '../../model/punto-control';
import { LecturaControl, PuntoControlDashboard, TurnoTrabajo } from '../../model/lectura-control';
import { Empresa } from '../../model/empresa';
import { calcularPaginasVisibles } from '../../shared/pagination.util';

// 🔥 Pantalla "Control de Turno": administracion del catalogo de
// puntos de control (temperatura, humedad, etc.), registro de lecturas
// horarias por turno, y dashboard con los graficos de evolucion y de
// cumplimiento de rango -- reemplaza el registro en planilla Excel
// (SISTEMA_DE_CONTROL_DE_MANTENCION). Visible tambien para TECNICO
// (a diferencia de la mayoria de las pantallas de "gestion"), porque
// es quien registra las lecturas en terreno.
@Component({
  selector: 'app-control-turno',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, MatAutocompleteModule, NgChartsModule],
  templateUrl: './control-turno.component.html',
  styleUrl: './control-turno.component.css'
})
export class ControlTurnoComponent implements OnInit {

  authService = inject(AuthService);
  controlTurnoService = inject(ControlTurnoService);
  empresaService = inject(EmpresaService);
  fb = inject(FormBuilder);

  esSuperAdmin = false;
  esAdminEmpresa = false;
  esAdminCatalogo = false; // SUPER_ADMIN o ADMIN_EMPRESA: administra el catalogo de puntos
  // 🔥 Mismos roles que el backend exige en /lecturas/importar-excel
  // (sin TECNICO: la importacion puede crear puntos de control nuevos).
  puedeImportarExcel = false;

  turnos: TurnoTrabajo[] = ['MANANA', 'TARDE', 'NOCHE'];

  // ---------- Catalogo de puntos de control ----------
  puntoForm!: FormGroup;
  empresaControl = new FormControl();
  empresas: Empresa[] = [];
  empresasFiltradas: Empresa[] = [];
  puntos: PuntoControl[] = [];
  editandoPunto = false;
  puntoEditandoId: number | null = null;

  paginaPuntos = 0;
  sizePuntos = 10;
  totalPagesPuntos = 0;
  totalElementsPuntos = 0;
  filtroNombrePunto = '';

  // ---------- Registro de lectura ----------
  lecturaForm!: FormGroup;
  puntosActivos: PuntoControl[] = [];
  puntoLecturaControl = new FormControl();
  puntosActivosFiltrados: PuntoControl[] = [];

  // ---------- Historial ----------
  lecturas: LecturaControl[] = [];
  filtroPuntoId: number | null = null;
  filtroPuntoControl = new FormControl();
  filtroPuntosFiltrados: PuntoControl[] = [];
  filtroDesde: string | null = null;
  filtroHasta: string | null = null;
  filtroTurno: TurnoTrabajo | null = null;

  page = 0;
  size = 10;
  totalPages = 0;
  totalElements = 0;

  // ---------- Dashboard ----------
  // 🔥 Filtro de fecha propio del dashboard, independiente del filtro
  // "Desde"/"Hasta" del Historial de arriba: son secciones distintas
  // (el Historial es un listado paginado, el dashboard son graficos
  // agregados) y antes compartian el mismo filtro sin que se notara,
  // lo que era confuso (para cambiar el rango de los graficos habia
  // que ir a mover un campo que visualmente pertenecia al Historial).
  // Si quedan ambos en null, el backend usa el dia actual (ver
  // LecturaControlServiceImpl.dashboard()).
  dashboardDesde: string | null = null;
  dashboardHasta: string | null = null;
  dashboard: PuntoControlDashboard[] = [];
  lineCharts: any[] = [];
  donaCharts: any[] = [];

  // 🔥 Plugin de datalabels: se pasa SOLO a los graficos de dona (via
  // [plugins] en el .html), no se registra global -- los graficos de
  // linea (lineCharts) siguen sin numeros encima de cada punto, tal
  // como estaban.
  donaPlugins = [ChartDataLabels];

  ngOnInit(): void {

    this.esSuperAdmin = this.authService.isAdmin();
    this.esAdminEmpresa = this.authService.isAdminEmpresa();
    this.esAdminCatalogo = this.esSuperAdmin || this.esAdminEmpresa;

    // 🔒 SUPER_ADMIN bypasea el flag de empresa (mismo criterio que
    // codigoQrHabilitado/controlTurnoHabilitado): el resto solo ve el
    // botón si, además del rol, su empresa tiene el Excel habilitado
    // (Empresa.hojaControlHabilitado) -- el parser es específico al
    // layout de una planilla real de una empresa puntual, no genérico.
    const tieneRolParaImportar =
      this.esAdminCatalogo || this.authService.getUserRole() === 'JEFE_MANTENIMIENTO';
    this.puedeImportarExcel =
      this.esSuperAdmin || (tieneRolParaImportar && !!this.authService.getHojaControlHabilitado());

    this.initPuntoForm();
    this.initLecturaForm();
    this.initAutocompletePuntoLectura();
    this.initAutocompleteFiltroPunto();

    if (this.esAdminCatalogo) {
      this.cargarEmpresas();
      this.initAutocompleteEmpresa();
      this.cargarPuntos();
    }

    this.cargarPuntosActivos();
    this.cargarLecturas();
    this.cargarDashboard();
  }

  // ===================== CATALOGO =====================

  initPuntoForm(): void {
    this.puntoForm = this.fb.group({
      nombre: ['', Validators.required],
      unidad: ['', Validators.required],
      valorMin: [null],
      valorMax: [null],
      empresaId: [null, Validators.required]
    });
  }

  initAutocompleteEmpresa(): void {
    this.empresaControl.valueChanges.subscribe(value => {
      const esObjeto = value && typeof value === 'object';
      const search = (esObjeto ? value.nombre : value || '').toLowerCase().trim();

      this.empresasFiltradas = !search
        ? this.empresas
        : this.empresas.filter(e => e.nombre.toLowerCase().includes(search));

      if (esObjeto) {
        this.puntoForm.patchValue({ empresaId: value.id });
      }
    });
  }

  cargarEmpresas(): void {
    this.empresaService.getAll().subscribe(data => {
      this.empresas = data;
      this.empresasFiltradas = data;

      // 🔥 ADMIN_EMPRESA solo administra su propia empresa: se
      // precarga automaticamente (mismo criterio que otros
      // mantenedores de catalogo, ej. Ubicacion).
      if (this.esAdminEmpresa) {
        const propia = data.find(e => e.id === this.authService.getEmpresaId());
        if (propia) {
          this.puntoForm.patchValue({ empresaId: propia.id });
          this.empresaControl.setValue(propia, { emitEvent: false });
        }
      }
    });
  }

  onFocusEmpresa(): void {
    this.empresasFiltradas = this.empresas;
  }

  displayEmpresa = (empresa: any): string => empresa?.nombre ?? '';

  cargarPuntos(): void {
    this.controlTurnoService
      .getPuntos(
        this.paginaPuntos,
        this.sizePuntos,
        this.esSuperAdmin ? null : this.authService.getEmpresaId(),
        this.filtroNombrePunto || undefined
      )
      .subscribe(res => {
        this.puntos = res.content;
        this.paginaPuntos = res.page.number;
        this.totalPagesPuntos = res.page.totalPages;
        this.totalElementsPuntos = res.page.totalElements;
      });
  }

  onFiltroNombrePuntoChange(): void {
    this.paginaPuntos = 0;
    this.cargarPuntos();
  }

  cambiarPaginaPuntos(p: number): void {
    if (p < 0 || p >= this.totalPagesPuntos) {
      return;
    }
    this.paginaPuntos = p;
    this.cargarPuntos();
  }

  paginasVisiblesPuntos(): number[] {
    return calcularPaginasVisibles(this.paginaPuntos, this.totalPagesPuntos);
  }

  guardarPunto(): void {

    if (this.puntoForm.invalid) {
      this.puntoForm.markAllAsTouched();
      return;
    }

    const valor = this.puntoForm.value;

    const request$ = this.editandoPunto && this.puntoEditandoId
      ? this.controlTurnoService.actualizarPunto(this.puntoEditandoId, valor)
      : this.controlTurnoService.crearPunto(valor);

    request$.subscribe({
      next: () => {
        Swal.fire('Listo', 'Punto de control guardado correctamente', 'success');
        this.nuevoPunto();
        this.cargarPuntos();
        this.cargarPuntosActivos();
        this.cargarDashboard();
      },
      error: (err) => {
        Swal.fire('Error', err.error?.error || 'No se pudo guardar el punto de control', 'error');
      }
    });
  }

  editarPunto(punto: PuntoControl): void {
    this.editandoPunto = true;
    this.puntoEditandoId = punto.id!;
    this.puntoForm.patchValue(punto);
    if (punto.empresaId) {
      const empresa = this.empresas.find(e => e.id === punto.empresaId);
      if (empresa) {
        this.empresaControl.setValue(empresa, { emitEvent: false });
      }
    }
  }

  nuevoPunto(): void {
    this.editandoPunto = false;
    this.puntoEditandoId = null;
    this.puntoForm.reset({ valorMin: null, valorMax: null });
    if (this.esAdminEmpresa) {
      this.puntoForm.patchValue({ empresaId: this.authService.getEmpresaId() });
    }
  }

  eliminarPunto(id: number): void {
    Swal.fire({
      title: '¿Deshabilitar punto de control?',
      text: 'Ya no aparecerá disponible para registrar nuevas lecturas.',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, deshabilitar'
    }).then(result => {
      if (result.isConfirmed) {
        this.controlTurnoService.eliminarPunto(id).subscribe(() => {
          this.cargarPuntos();
          this.cargarPuntosActivos();
          this.cargarDashboard();
        });
      }
    });
  }

  habilitarPunto(id: number): void {
    Swal.fire({
      title: '¿Habilitar punto de control?',
      text: 'Volverá a estar disponible para registrar lecturas.',
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: 'Sí, habilitar'
    }).then(result => {
      if (result.isConfirmed) {
        this.controlTurnoService.habilitarPunto(id).subscribe(() => {
          this.cargarPuntos();
          this.cargarPuntosActivos();
          this.cargarDashboard();
        });
      }
    });
  }

  // ===================== REGISTRO DE LECTURA =====================

  initLecturaForm(): void {
    this.lecturaForm = this.fb.group({
      puntoControlId: [null, Validators.required],
      valor: [null, Validators.required],
      turno: [null, Validators.required],
      fechaHora: [this.horaActualInput()],
      observacion: ['']
    });
  }

  private horaActualInput(): string {
    const ahora = new Date();
    ahora.setMinutes(ahora.getMinutes() - ahora.getTimezoneOffset());
    return ahora.toISOString().slice(0, 16);
  }

  cargarPuntosActivos(): void {
    this.controlTurnoService.getPuntosActivos().subscribe(data => {
      this.puntosActivos = data;
      this.puntosActivosFiltrados = data;
      this.filtroPuntosFiltrados = data;
    });
  }

  // 🔥 Autocompletado "escribir para buscar" del punto de control en el
  // formulario de registro de lectura (mismo patron que Empresa, ver
  // initAutocompleteEmpresa): al elegir una opcion de la lista, se
  // guarda el id real en lecturaForm.puntoControlId; si se borra el
  // texto, se limpia el id para que la validacion "required" lo pesque.
  initAutocompletePuntoLectura(): void {
    this.puntoLecturaControl.valueChanges.subscribe(value => {
      const esObjeto = value && typeof value === 'object';
      const search = (esObjeto ? value.nombre : value || '').toLowerCase().trim();

      this.puntosActivosFiltrados = !search
        ? this.puntosActivos
        : this.puntosActivos.filter(p => p.nombre.toLowerCase().includes(search));

      if (esObjeto) {
        this.lecturaForm.patchValue({ puntoControlId: value.id });
      } else if (!search) {
        this.lecturaForm.patchValue({ puntoControlId: null });
      }
    });
  }

  onFocusPuntoLectura(): void {
    this.puntosActivosFiltrados = this.puntosActivos;
  }

  // 🔥 Mismo autocompletado, pero para el filtro de punto de control
  // del historial (equivalente a filtroEmpresaControl en Ubicacion):
  // al elegir/borrar dispara aplicarFiltros() igual que antes hacia el
  // <select> con (change).
  initAutocompleteFiltroPunto(): void {
    this.filtroPuntoControl.valueChanges.subscribe(value => {
      const esObjeto = value && typeof value === 'object';
      const search = (esObjeto ? value.nombre : value || '').toLowerCase().trim();

      this.filtroPuntosFiltrados = !search
        ? this.puntosActivos
        : this.puntosActivos.filter(p => p.nombre.toLowerCase().includes(search));

      if (esObjeto) {
        this.filtroPuntoId = value.id;
        this.aplicarFiltros();
      } else if (!search && this.filtroPuntoId !== null) {
        this.filtroPuntoId = null;
        this.aplicarFiltros();
      }
    });
  }

  onFocusFiltroPunto(): void {
    this.filtroPuntosFiltrados = this.puntosActivos;
  }

  displayPuntoControl = (punto: any): string => punto ? `${punto.nombre} (${punto.unidad})` : '';

  registrarLectura(): void {

    if (this.lecturaForm.invalid) {
      this.lecturaForm.markAllAsTouched();
      return;
    }

    this.controlTurnoService.registrarLectura(this.lecturaForm.value).subscribe({
      next: () => {
        Swal.fire('Listo', 'Lectura registrada correctamente', 'success');
        this.lecturaForm.reset({ fechaHora: this.horaActualInput() });
        this.puntoLecturaControl.setValue('', { emitEvent: false });
        this.cargarLecturas();
        this.cargarDashboard();
      },
      error: (err) => {
        Swal.fire('Error', err.error?.error || 'No se pudo registrar la lectura', 'error');
      }
    });
  }

  // 🔥 Carga masiva desde la planilla real "HOJA DE CONTROL" (hoja de
  // Excel con layout fijo, ver HojaControlImportServiceImpl): crea las
  // lecturas de HOY para cada punto/hora que trae el archivo. Se puede
  // volver a subir el mismo archivo sin duplicar (el backend omite las
  // lecturas que ya existen para ese punto+hora).
  importandoExcel = false;

  importarExcel(event: Event): void {

    const input = event.target as HTMLInputElement;
    const archivo = input.files?.[0];

    if (!archivo) {
      return;
    }

    this.importandoExcel = true;

    this.controlTurnoService.importarExcel(archivo).subscribe({
      next: (resultado) => {
        this.importandoExcel = false;
        input.value = '';

        const puntosNuevos = resultado.puntosNuevosCreados.length
          ? `<br><br>Puntos de control nuevos creados: ${resultado.puntosNuevosCreados.join(', ')}`
          : '';

        Swal.fire({
          icon: 'success',
          title: 'Importación completada',
          html: `${resultado.lecturasCreadas} lectura(s) creada(s), `
              + `${resultado.lecturasOmitidas} ya existían y se omitieron.${puntosNuevos}`
        });

        this.cargarPuntosActivos();
        this.cargarLecturas();
        this.cargarDashboard();
      },
      error: (err) => {
        this.importandoExcel = false;
        input.value = '';

        Swal.fire({
          icon: 'error',
          title: err.error?.error || 'No fue posible importar el archivo'
        });
      }
    });
  }

  // ===================== HISTORIAL =====================

  cargarLecturas(): void {
    this.controlTurnoService.getLecturas(
      this.page, this.size, this.filtroPuntoId, this.filtroDesde, this.filtroHasta, this.filtroTurno
    ).subscribe(res => {
      this.lecturas = res.content;
      this.totalPages = res.page.totalPages;
      this.totalElements = res.page.totalElements;
    });
  }

  // 🔥 "Punto de control" y "Turno" siguen compartidos entre Historial
  // y Dashboard a proposito (tiene sentido ver el mismo punto/turno
  // filtrado en ambos lados a la vez) -- por eso aca se siguen
  // recargando los dos. Lo unico que se separo fue la fecha (ver
  // aplicarFiltroDashboard), porque mezclar el rango del Historial con
  // el del dashboard era confuso (eran secciones distintas).
  aplicarFiltros(): void {
    this.page = 0;
    this.cargarLecturas();
    this.cargarDashboard();
  }

  // 🔥 Filtro de fecha propio del dashboard (ver comentario en la
  // declaracion de dashboardDesde/dashboardHasta): solo recarga el
  // dashboard, no toca la paginacion ni el listado del Historial.
  aplicarFiltroDashboard(): void {
    this.cargarDashboard();
  }

  cambiarPagina(p: number): void {
    if (p < 0 || p >= this.totalPages) {
      return;
    }
    this.page = p;
    this.cargarLecturas();
  }

  paginasVisibles(): number[] {
    return calcularPaginasVisibles(this.page, this.totalPages);
  }

  trackByLecturaId(_: number, l: LecturaControl): number | undefined {
    return l.id;
  }

  // ===================== DASHBOARD =====================

  cargarDashboard(): void {
    this.controlTurnoService.getDashboard(
      this.filtroPuntoId, this.dashboardDesde, this.dashboardHasta, this.filtroTurno
    ).subscribe(data => {
      this.dashboard = data;
      this.construirGraficos();
    });
  }

  private construirGraficos(): void {

    // 🔥 Los puntos cuyo nombre termina en "Sección N°X" (ej. "Proofer 1
    // - Temperatura Sección N°1") se agrupan en UN solo grafico de linea
    // por prefijo comun (ej. "Proofer 1 - Temperatura"), con una serie
    // de color distinto por seccion -- replica los graficos combinados
    // del Excel original (ver seed V30). Los puntos que no siguen ese
    // patron (camaras, salas, tiempo de fermentacion, velocidades)
    // siguen mostrandose cada uno en su propio grafico, como antes.
    const COLORES = ['#3f51b5', '#e91e63', '#009688', '#ff9800', '#795548', '#607d8b'];
    const patronSeccion = /^(.*)\s+(Sección N°\d+)$/i;

    // 🔥 Estos 5 puntos no siguen el patron "... Sección N°X" (no son
    // secciones de un mismo equipo, son sensores sueltos de camaras y
    // salas distintas), asi que su agrupacion se define a mano en vez
    // de por regex -- replica los otros 2 graficos combinados del
    // Excel original ("TEMPERATURA CAMARAS DE CONGELADO" y
    // "TEMPERATURA DE SALAS DE PROCESOS", ver V30 para los nombres
    // exactos de los puntos).
    const GRUPOS_FIJOS: Record<string, string> = {
      'Cámara Variedades 1': 'Cámaras de Congelado',
      'Cámara Variedades 2': 'Cámaras de Congelado',
      'Cámara de Congelado': 'Cámaras de Congelado',
      'Sala de Envasado': 'Salas de Procesos',
      'Sala de Variedades': 'Salas de Procesos'
    };

    const grupos = new Map<string, { unidad: string; miembros: { etiqueta: string; punto: PuntoControlDashboard }[] }>();
    const individuales: PuntoControlDashboard[] = [];

    for (const punto of this.dashboard) {

      const grupoFijo = GRUPOS_FIJOS[punto.nombre];
      const match = punto.nombre.match(patronSeccion);

      let titulo: string;
      let etiqueta: string;

      if (grupoFijo) {
        titulo = grupoFijo;
        etiqueta = punto.nombre;
      } else if (match) {
        titulo = match[1];
        etiqueta = match[2];
      } else {
        individuales.push(punto);
        continue;
      }

      if (!grupos.has(titulo)) {
        grupos.set(titulo, { unidad: punto.unidad, miembros: [] });
      }

      grupos.get(titulo)!.miembros.push({ etiqueta, punto });
    }

    const chartsAgrupados = Array.from(grupos.entries()).map(([titulo, grupo]) =>
      this.armarLineChart(
        titulo,
        grupo.unidad,
        grupo.miembros
          .sort((a, b) => a.etiqueta.localeCompare(b.etiqueta, undefined, { numeric: true }))
          .map((m, i) => ({ etiqueta: m.etiqueta, punto: m.punto, color: COLORES[i % COLORES.length] }))
      )
    );

    // 🔥 Estos 5 puntos SI son dona en el Excel original (no linea):
    // cada hora del turno es una porcion de la dona, de tamaño
    // proporcional al valor leido en esa hora (ver
    // armarDonaPorHora) -- a diferencia de la dona "% dentro de
    // rango" que se arma mas abajo para cualquier punto con
    // valorMin/valorMax, esta replica fielmente la forma del grafico
    // original de la planilla para estos puntos puntuales.
    const PUNTOS_DONA_POR_HORA = new Set([
      'Tiempo de Fermentación Proofer N°1',
      'Tiempo de Fermentación Proofer N°2',
      'Velocidad Espirales de Enfriado y Congelado',
      'Velocidad Freidora N°1',
      'Velocidad Freidora N°2'
    ]);

    const individualesLinea = individuales.filter(p => !PUNTOS_DONA_POR_HORA.has(p.nombre));
    const individualesDonaPorHora = individuales.filter(p => PUNTOS_DONA_POR_HORA.has(p.nombre));

    const chartsIndividuales = individualesLinea.map(punto =>
      this.armarLineChart(punto.nombre, punto.unidad, [
        { etiqueta: punto.nombre, punto, color: COLORES[0] }
      ])
    );

    // 🔥 La dona por hora solo tiene sentido para el turno/dia actual
    // (18 porciones como maximo, una por hora): con mas de un dia de
    // datos filtrados se volveria una dona con cientos de porciones,
    // asi que estos 5 puntos pasan a graficarse como linea (con
    // promedio diario, ver armarLineChart) igual que el resto.
    const diasUnicosGlobal = new Set(
      this.dashboard.flatMap(p => p.fechas.map(f => new Date(f).toDateString()))
    );
    const esRangoAmplio = diasUnicosGlobal.size > 1;

    const donaPorHoraCharts = esRangoAmplio
      ? []
      : individualesDonaPorHora
          .filter(p => p.fechas.length > 0)
          .map(punto => this.armarDonaPorHora(punto));

    const lineChartsDonaPorHora = esRangoAmplio
      ? individualesDonaPorHora.map(punto =>
          this.armarLineChart(punto.nombre, punto.unidad, [
            { etiqueta: punto.nombre, punto, color: COLORES[0] }
          ])
        )
      : [];

    this.lineCharts = [...chartsAgrupados, ...chartsIndividuales, ...lineChartsDonaPorHora];

    // 🔥 Dona "% dentro de rango": se arma para cualquier punto con
    // valorMin/valorMax definido y al menos una lectura (no incluye
    // los 5 de arriba, que ya tienen su propia dona por hora arriba).
    // No se agrupan por seccion -- cada punto con rango mantiene su
    // propia dona.
    const donaRangoCharts = this.dashboard
      .filter(p => !PUNTOS_DONA_POR_HORA.has(p.nombre))
      .filter(p => p.valorMin != null && p.valorMax != null && (p.lecturasDentroRango + p.lecturasFueraRango) > 0)
      .map(punto => ({
        punto,
        subtitulo: `${punto.nombre} — % dentro de rango`,
        type: 'doughnut' as ChartType,
        data: {
          labels: ['Dentro de rango', 'Fuera de rango'],
          datasets: [{
            data: [punto.lecturasDentroRango, punto.lecturasFueraRango],
            backgroundColor: ['#4caf50', '#e57373']
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: { display: true },
            // 🔥 Numero de lecturas encima de cada porcion (dentro vs
            // fuera de rango). Se oculta el "0" cuando una porcion no
            // tiene lecturas, para no ensuciar la dona con un cero
            // flotando sobre una porcion de tamaño 0.
            datalabels: {
              color: '#fff',
              font: { weight: 'bold', size: 12 },
              formatter: (value: number) => value > 0 ? value : null
            }
          }
        }
      }));

    this.donaCharts = [...donaPorHoraCharts, ...donaRangoCharts];
  }

  // 🔥 Dona fiel al Excel original para "Tiempo de Fermentación
  // Proofer N°1/2" y "Velocidad Espirales/Freidora N°1/2": cada
  // porcion es una hora del turno, de tamaño proporcional al valor
  // leido en esa hora (en el Excel eran 18 columnas horarias con un
  // valor cada una -- aca son las lecturas reales del punto en el
  // rango filtrado).
  private armarDonaPorHora(punto: PuntoControlDashboard): any {

    const COLORES_HORA = [
      '#3f51b5', '#e91e63', '#009688', '#ff9800', '#795548', '#607d8b',
      '#9c27b0', '#00bcd4', '#8bc34a', '#ffc107', '#f44336', '#3f51b5'
    ];

    const labels = punto.fechas.map(f =>
      new Date(f).toLocaleString('es-CL', { hour: '2-digit', minute: '2-digit' })
    );

    return {
      punto,
      subtitulo: `${punto.nombre} — Valor por hora (${punto.unidad})`,
      type: 'doughnut' as ChartType,
      data: {
        labels,
        datasets: [{
          data: punto.valores,
          backgroundColor: labels.map((_, i) => COLORES_HORA[i % COLORES_HORA.length])
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: true, position: 'right' },
          // 🔥 Valor de la lectura encima de cada porcion (una porcion
          // por hora). Con hasta 18 lecturas en un turno las porciones
          // chicas pueden quedar apretadas -- por eso el texto va en
          // tamaño reducido, igual sigue siendo mas rapido de leer que
          // tener que pasar el mouse porcion por porcion.
          datalabels: {
            color: '#fff',
            font: { weight: 'bold', size: 10 },
            formatter: (value: number) => value
          }
        }
      }
    };
  }

  // 🔥 Arma un unico grafico de linea a partir de 1 o mas series. Cada
  // serie puede traer sus propias fechas (no siempre se registran los
  // sensores de un grupo exactamente en el mismo instante): se arma un
  // eje de tiempo unico con la union ordenada de todas las fechas del
  // grupo, y cada serie se alinea a ese eje dejando "null" donde no
  // tiene lectura en ese instante exacto (spanGaps une la linea igual,
  // en vez de cortarla).
  //
  // 🔥 Con mas de un dia de datos en el rango filtrado, graficar cada
  // lectura individual satura el eje X (cientos de puntos, horas
  // repetidas e ilegibles) -- se agrega a promedio diario en su lugar.
  // Para ver el detalle hora a hora hay que acotar el filtro a un dia.
  // 🔥 forzarDetalle=true se usa para el modal "Ver detalle" (abrirModalChart):
  // rearma el mismo grafico sin agregar por dia, aunque el rango sea
  // amplio, para mostrar cada lectura individual en una vista mas grande.
  // 🔥 soloDia (toDateString()) acota ademas esa vista de detalle a UN
  // solo dia -- el carrusel del modal reconstruye el grafico dia por
  // dia en vez de mostrar todo el rango junto (ver modalMoverDia).
  private armarLineChart(
    titulo: string,
    unidad: string,
    series: { etiqueta: string; punto: PuntoControlDashboard; color: string }[],
    forzarDetalle = false,
    soloDia?: string
  ): any {

    const seriesFiltradas = !soloDia ? series : series.map(s => {
      const indices = s.punto.fechas
        .map((f, i) => i)
        .filter(i => new Date(s.punto.fechas[i]).toDateString() === soloDia);

      return {
        ...s,
        punto: {
          ...s.punto,
          fechas: indices.map(i => s.punto.fechas[i]),
          valores: indices.map(i => s.punto.valores[i])
        }
      };
    });

    const fechasUnicas = Array.from(new Set(seriesFiltradas.flatMap(s => s.punto.fechas))).sort();
    const diasUnicos = new Set(fechasUnicas.map(f => new Date(f).toDateString()));
    const agregarPorDia = !forzarDetalle && diasUnicos.size > 1;

    let labels: string[];
    let datasets: any[];

    if (agregarPorDia) {

      const diasOrdenados = Array.from(diasUnicos)
        .sort((a, b) => new Date(a).getTime() - new Date(b).getTime());

      labels = diasOrdenados.map(dia =>
        new Date(dia).toLocaleString('es-CL', { day: '2-digit', month: '2-digit' })
      );

      datasets = seriesFiltradas.map(s => {

        const totalesPorDia = new Map<string, { suma: number; cantidad: number }>();

        s.punto.fechas.forEach((f, i) => {
          const dia = new Date(f).toDateString();
          const acumulado = totalesPorDia.get(dia) ?? { suma: 0, cantidad: 0 };
          acumulado.suma += s.punto.valores[i];
          acumulado.cantidad += 1;
          totalesPorDia.set(dia, acumulado);
        });

        return {
          data: diasOrdenados.map(dia => {
            const acumulado = totalesPorDia.get(dia);
            return acumulado
              ? Math.round((acumulado.suma / acumulado.cantidad) * 100) / 100
              : null;
          }),
          label: seriesFiltradas.length > 1 ? s.etiqueta : `${s.etiqueta} (${unidad})`,
          fill: false,
          spanGaps: true,
          tension: 0.4,
          borderColor: s.color,
          backgroundColor: s.color
        };
      });

    } else {

      // 🔥 Eje X = hora de la lectura (HH:mm), como en la planilla
      // original ("HORA DE MEDICION"). Si igual hay mas de un dia
      // (forzarDetalle=true desde el modal "Ver detalle" con un rango
      // amplio), se antepone la fecha para no repetir la misma hora sin
      // poder distinguir a que dia pertenece cada una.
      labels = fechasUnicas.map(f => {
        const fecha = new Date(f);
        return diasUnicos.size > 1
          ? fecha.toLocaleString('es-CL', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' })
          : fecha.toLocaleString('es-CL', { hour: '2-digit', minute: '2-digit' });
      });

      datasets = seriesFiltradas.map(s => {
        const valorPorFecha = new Map(s.punto.fechas.map((f, i) => [f, s.punto.valores[i]]));
        return {
          data: fechasUnicas.map(f => valorPorFecha.has(f) ? valorPorFecha.get(f) : null),
          label: seriesFiltradas.length > 1 ? s.etiqueta : `${s.etiqueta} (${unidad})`,
          fill: false,
          spanGaps: true,
          tension: 0.4,
          borderColor: s.color,
          backgroundColor: s.color
        };
      });
    }

    return {
      punto: { nombre: titulo, unidad },
      type: 'line' as ChartType,
      data: { labels, datasets },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: true } },
        scales: {
          x: {
            title: { display: true, text: agregarPorDia ? 'Día (promedio)' : 'Hora' },
            ticks: { autoSkip: true, maxRotation: 0 }
          },
          y: { title: { display: true, text: unidad } }
        }
      },
      // 🔥 Metadata propia (Chart.js/ng2-charts la ignora, solo lee
      // type/data/options): agregadoPorDia habilita el "Ver detalle" en
      // el template, y _titulo/_unidad/_series permiten rearmar este
      // mismo grafico sin agregar (ver abrirModalChart).
      agregadoPorDia: agregarPorDia,
      _titulo: titulo,
      _unidad: unidad,
      _series: series
    };
  }

  // ===================== MODAL "VER DETALLE" =====================

  modalChart: any = null;

  // 🔥 Carrusel de dias dentro del modal: en vez de mostrar TODAS las
  // lecturas del rango amplio amuchadas en un solo grafico (ilegible
  // con varios dias), se navega dia por dia -- estos 4 campos guardan
  // el "molde" (titulo/unidad/series completas) y en que dia esta
  // parado el carrusel para poder rearmar el grafico de cada dia.
  private modalTitulo = '';
  private modalUnidad = '';
  private modalSeries: { etiqueta: string; punto: PuntoControlDashboard; color: string }[] = [];
  modalDiasDisponibles: string[] = [];
  modalDiaIndex = 0;

  // 🔥 Solo tiene sentido para gráficos agregados por día (rango
  // amplio): rearma el mismo grafico en detalle horario completo
  // (forzarDetalle=true), para verlo mas grande sin perder informacion.
  // Con mas de un dia de datos, en vez de un solo grafico con todo
  // junto, arranca el carrusel dia por dia (ver modalMoverDia) --
  // parado por defecto en el dia MAS RECIENTE del rango.
  abrirModalChart(chart: any): void {

    if (!chart.agregadoPorDia) {
      return;
    }

    this.modalTitulo = chart._titulo;
    this.modalUnidad = chart._unidad;
    this.modalSeries = chart._series;

    const fechasUnicas = Array.from(new Set(
      (chart._series as { punto: PuntoControlDashboard }[]).flatMap(s => s.punto.fechas)
    ));

    this.modalDiasDisponibles = Array.from(new Set(fechasUnicas.map(f => new Date(f).toDateString())))
      .sort((a, b) => new Date(a).getTime() - new Date(b).getTime());

    this.modalDiaIndex = this.modalDiasDisponibles.length - 1;

    this.actualizarModalChart();
  }

  private actualizarModalChart(): void {
    const dia = this.modalDiasDisponibles[this.modalDiaIndex];
    this.modalChart = this.armarLineChart(this.modalTitulo, this.modalUnidad, this.modalSeries, true, dia);
  }

  modalMoverDia(direccion: 1 | -1): void {

    const nuevoIndex = this.modalDiaIndex + direccion;

    if (nuevoIndex < 0 || nuevoIndex >= this.modalDiasDisponibles.length) {
      return;
    }

    this.modalDiaIndex = nuevoIndex;
    this.actualizarModalChart();
  }

  get modalDiaLabel(): string {

    const dia = this.modalDiasDisponibles[this.modalDiaIndex];

    if (!dia) {
      return '';
    }

    const etiqueta = new Date(dia).toLocaleDateString('es-CL', {
      weekday: 'long', day: '2-digit', month: 'long'
    });

    return etiqueta.charAt(0).toUpperCase() + etiqueta.slice(1);
  }

  cerrarModalChart(): void {
    this.modalChart = null;
    this.modalDiasDisponibles = [];
    this.modalDiaIndex = 0;
  }
}
