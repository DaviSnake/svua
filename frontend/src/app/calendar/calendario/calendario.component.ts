import { Component, HostListener, inject, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { Subscription } from 'rxjs';
import { CalendarOptions } from '@fullcalendar/core';

import { FullCalendarComponent, FullCalendarModule } from '@fullcalendar/angular';

import dayGridPlugin from '@fullcalendar/daygrid';
import interactionPlugin from '@fullcalendar/interaction';
import timeGridPlugin from '@fullcalendar/timegrid';
import esLocale from '@fullcalendar/core/locales/es';
import listPlugin from '@fullcalendar/list';

import { OrdenMantencionService } from '../../services/orden-mantencion.service';
import { OrdenRepuestoService } from '../../services/orden-repuesto.service';
import { FormArray, FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ActivoService } from '../../services/activo.service';
import { Activo } from '../../model/activo';
import { OrdenResponse } from '../../model/ordenResponse';

import { MatAutocompleteModule, MatAutocompleteTrigger } from '@angular/material/autocomplete';
import { AuthService } from '../../services/auth.service';
import Swal from 'sweetalert2';
import { RepuestoService } from '../../services/repuesto.service';
import { ProveedorService } from '../../services/proveedor.service';
import { FormUtils } from '../../shared/form-utils';
import { DomSanitizer, SafeResourceUrl, SafeUrl } from '@angular/platform-browser';
import { EmpresaService } from '../../services/empresa.service';
import { Empresa } from '../../model/empresa';

@Component({
  selector: 'app-calendario',
  standalone: true,
  imports: [FullCalendarModule, CommonModule, ReactiveFormsModule, MatAutocompleteModule],
  templateUrl: './calendario.component.html',
  styleUrls: ['./calendario.component.css']
})
export class CalendarioComponent implements OnInit, OnDestroy {

  // 🔥 user$ es un BehaviorSubject de por-vida-de-la-app (AuthService);
  // guardamos la suscripción para liberarla en ngOnDestroy y que no
  // quede activa después de salir del calendario.
  private usuarioSub?: Subscription;

  private ordenMantencionService = inject(OrdenMantencionService);
  private ordenRepuestoService = inject(OrdenRepuestoService);
  private activoService = inject(ActivoService);
  private repuestoService = inject(RepuestoService);
  private proveedorService = inject(ProveedorService);
  private authService = inject(AuthService);
  private empresaService = inject(EmpresaService);
  private sanitizer = inject(DomSanitizer);
  private fb = inject(FormBuilder);

  usuario: any;

  // 🔥 Filtro empresa: solo lo ve y lo puede usar el SUPER_ADMIN (mismo
  // patrón que en Dashboard/Reportes/Informe de Mantenciones); para el
  // resto de los roles el calendario sigue mostrando solo su propia
  // empresa, como antes.
  esSuperAdmin = false;

  // 🔥 Ingreso retroactivo de ordenes (solo SUPER_ADMIN / ADMIN_EMPRESA,
  // ver OrdenMantenimientoServiceImpl.construirOrdenRetroactiva).
  esAdminEmpresa = false;
  puedeIngresoRetroactivo = false;
  empresas: Empresa[] = [];
  empresasFiltradas: Empresa[] = [];
  filtroEmpresaControl = new FormControl();
  filtroEmpresaId: number | null = null;

  activos: Activo[] = [];

  repuestos: any[] = [];
  repuestosAsociados: any[] = [];

  proveedores: any[] = [];

  activoControl = new FormControl();
  activosFiltrados: Activo[] = [];

  // 🔥 Autocompletado "escribir para buscar" (mismo patrón que activoControl),
  // manteniendo el envío por ID hacia el backend (proveedorId).
  proveedorControl = new FormControl();
  proveedoresFiltrados: any[] = [];

  // 🔥 "Tipo" es una lista fija (no viene de una tabla), pero igual se
  // muestra como autocompletado por consistencia visual con el resto.
  tiposMantenimiento = ['PREVENTIVO', 'CORRECTIVO', 'PREDICTIVO'];
  tiposMantenimientoFiltrados: string[] = this.tiposMantenimiento;
  tipoMantenimientoControl = new FormControl();

  estadoOrden: string = 'PENDIENTE';

  // 🔥 indica si la orden actualmente seleccionada ya tiene un
  // checklist adjunto (independiente del archivo en sí).
  tieneChecklist: boolean = false;

  riesgo!: number;
  nivel!: string;
  costoTotal!: string;

  orden!: OrdenResponse;

  @ViewChild('calendar') calendarComponent!: FullCalendarComponent;
  @ViewChild(MatAutocompleteTrigger) trigger!: MatAutocompleteTrigger;

  ngAfterViewInit() {
    setTimeout(() => {
      // 🔥 getApi() puede devolver null si el calendario todavía no terminó
      // de montarse en este instante (por eso el segundo "?." — sin él,
      // "Cannot read properties of null (reading 'render')" podía romper
      // la carga de la pantalla).
      this.calendarComponent?.getApi()?.render();
      this.trigger?.openPanel();
    }, 300);
  }

  isMobile(): boolean {
    return window.innerWidth < 768;
  }

  // =====================================================
  // 🧠 RESPONSIVE CALENDAR VIEW
  // =====================================================

  private getCalendarView(): string {

    const width = window.innerWidth;

    if (width < 768) return 'listWeek';      // 📱 mobile
    if (width < 1024) return 'timeGridDay';  // 📲 tablet

    return 'timeGridWeek';                   // 💻 desktop
  }

  @HostListener('window:resize')
  onResize() {

    const api = this.calendarComponent?.getApi();
    if (!api) return;

    // 🔥 si el usuario eligió manualmente la vista Mes (dayGridMonth)
    // desde el header, no se la pisamos con el cambio automático de
    // vista responsive por ancho de pantalla.
    if (api.view.type === 'dayGridMonth') {
      setTimeout(() => api.updateSize(), 100);
      return;
    }

    api.changeView(this.getCalendarView());

    setTimeout(() => api.updateSize(), 100);
  }

  // =====================================================
  // 🎨 EVENT RENDER (TOOLTIP + STYLE)
  // =====================================================

  // 🔒 estado/tipoMantenimiento son campos de texto libre que vienen del
  // backend (no un enum cerrado en el DTO); se escapan antes de
  // interpolarlos en innerHTML para evitar XSS almacenado si alguna vez
  // contienen HTML/markup.
  private escapeHtml(valor: any): string {
    return String(valor ?? '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  private onEventDidMount(info: any) {

  const isMobile = window.innerWidth < 768;

  const estado = info.event.extendedProps?.estado;
  const tipo = info.event.extendedProps?.tipoMantenimiento;
  const duracion = info.event.extendedProps?.duracionMinutos;

  info.el.style.boxShadow = this.getShadowPorEstado(estado);
  info.el.style.borderRadius = '6px';
  info.el.style.border = 'none';
  info.el.style.cursor = 'pointer';
  info.el.style.transition = 'all .2s ease';

  // 👇 AQUÍ VA TU BLOQUE
  if (isMobile) {

    info.el.style.cursor = 'pointer';
    info.el.title = 'Toca para ver / crear';
  }

  // 🚫 MOBILE: sin tooltip hover
  if (isMobile) return;

  const tooltip = document.createElement('div');

  tooltip.innerHTML = `
    <strong>${this.escapeHtml(estado)}</strong><br>
    Tipo: ${this.escapeHtml(tipo)}<br>
    Duración: ${this.escapeHtml(duracion)} min
  `;

  Object.assign(tooltip.style, {
    position: 'absolute',
    bottom: '110%',
    left: '50%',
    transform: 'translateX(-50%)',
    background: '#111827',
    color: '#fff',
    padding: '8px 10px',
    borderRadius: '8px',
    fontSize: '12px',
    whiteSpace: 'nowrap',
    boxShadow: '0 4px 12px rgba(0,0,0,.25)',
    opacity: '0',
    pointerEvents: 'none',
    transition: 'opacity .2s ease',
    zIndex: '9999'
  });

  info.el.appendChild(tooltip);

  info.el.addEventListener('mouseenter', () => {
    tooltip.style.opacity = '1';
    info.el.style.transform = 'scale(1.03)';
  });

  info.el.addEventListener('mouseleave', () => {
    tooltip.style.opacity = '0';
    info.el.style.transform = 'scale(1)';
  });
}


  // 🔹 CALENDARIO (inicializado desde el inicio 🔥)
  calendarOptions: CalendarOptions = {
    plugins: [dayGridPlugin, timeGridPlugin, interactionPlugin, listPlugin],
    initialView: this.getCalendarView(),
    initialDate: new Date(), // ✅ semana actual
    // 🔥 botones para elegir Mes / Semana / Día manualmente (antes
    // solo se podía ver semana o día, cambiando automático según el
    // ancho de pantalla, sin opción de ver el mes completo).
    headerToolbar: {
      left: 'prev,next today',
      center: 'title',
      right: 'dayGridMonth,timeGridWeek,timeGridDay'
    },
    editable: true,
    selectable: true,   // 🔥 CLAVE
    navLinks: true,
    locale: esLocale,
    height: 'auto',   // 🔥 IMPORTANTE
    expandRows: true, // 🔥 IMPORTANTE
    contentHeight: 'auto',
    // 🔴 Línea horizontal que marca la hora exacta en tiempo real (solo se
    // dibuja en las vistas timeGridDay / timeGridWeek; FullCalendar la
    // actualiza sola mientras la vista esté montada).
    nowIndicator: true,
    events: [],

    eventDidMount: (info) => {
      const estado =
        info.event.extendedProps?.['estado'];

      const tipo =
        info.event.extendedProps?.['tipoMantenimiento'];

      const duracion =
        info.event.extendedProps?.['duracionMinutos'];

      info.el.style.boxShadow =
        this.getShadowPorEstado(estado);

      info.el.style.borderRadius = '6px';

      info.el.style.border = 'none';

      info.el.style.cursor = 'pointer';

      info.el.style.position = 'relative';

      const tooltip = document.createElement('div');

      tooltip.innerHTML = `
        <strong>${this.escapeHtml(estado)}</strong><br>
        Tipo: ${this.escapeHtml(tipo)}<br>
        Duración: ${this.escapeHtml(duracion)} min
      `;

      tooltip.style.position = 'absolute';
      tooltip.style.bottom = '110%';
      tooltip.style.left = '50%';
      tooltip.style.transform = 'translateX(-50%)';

      tooltip.style.background = '#111827';
      tooltip.style.color = '#fff';

      tooltip.style.padding = '8px 10px';
      tooltip.style.borderRadius = '8px';

      tooltip.style.fontSize = '12px';
      tooltip.style.whiteSpace = 'nowrap';

      tooltip.style.boxShadow =
        '0 4px 12px rgba(0,0,0,.25)';

      tooltip.style.opacity = '0';

      tooltip.style.pointerEvents = 'none';

      tooltip.style.transition = 'opacity .2s ease';

      tooltip.style.zIndex = '9999';

      info.el.appendChild(tooltip);

      info.el.addEventListener('mouseenter', () => {

        tooltip.style.opacity = '1';

        info.el.style.transform = 'scale(1.03)';
      });

      info.el.addEventListener('mouseleave', () => {

        tooltip.style.opacity = '0';

        info.el.style.transform = 'scale(1)';
      });

      info.el.style.transition = 'all .2s ease';
    },

    // 🟢 CREAR
    dateClick: (info) => this.onDateClick(info),

    // 🟡 MOVER
    eventDrop: (info) => this.onEventDrop(info),

    // 🔵 EDITAR
    eventClick: (info) => this.onEventClick(info),

    select: (info) => this.onSelectRange(info),
  };

  getShadowPorEstado(estado?: string): string {

  switch (estado) {

      case 'COMPLETADA':
        return '0 0 10px rgba(34,197,94,0.4)';

      case 'EN_EJECUCION':
        return '0 0 10px rgba(249,115,22,0.4)';

      case 'PROGRAMADA':
        return '0 0 10px rgba(59,130,246,0.4)';

      case 'PENDIENTE':
        return '0 0 10px rgba(234,179,8,0.4)';

      case 'CANCELADA':
        return '0 0 10px rgba(239,68,68,0.4)';

      default:
        return 'none';
    }
  }

  // 🔹 FORMULARIO
  ordenMantencionForm!: FormGroup;
  mostrarModal = false;
  repuestoForm!: FormGroup;
  mostrarModalRepuesto = false;
  fechaSeleccionada!: string;

  modoEdicion = false;
  eventoSeleccionadoId!: number;

  // 🔥 en mobile el checklist se sigue viendo dentro de un modal
  // (imagen inline o iframe); en desktop se abre directo en una
  // pestaña nueva (ver validarChecklist).
  mostrarModalArchivo = false;
  esImagenChecklist = false;
  archivoUrlImagen!: SafeUrl;
  archivoUrl!: SafeResourceUrl;

  page = 0;
  size = 100000;

  ngOnInit(): void {
    // usuario
    this.usuarioSub = this.authService.user$.subscribe(user => {
      this.usuario = user;
    });

    this.esSuperAdmin = this.authService.isAdmin();
    this.esAdminEmpresa = this.authService.isAdminEmpresa();
    this.puedeIngresoRetroactivo = this.esSuperAdmin || this.esAdminEmpresa;

    if (this.esSuperAdmin) {
      this.initFiltroEmpresa();
      this.cargarEmpresas();
    }

    this.ordenMantencionForm = this.fb.group({
      titulo: ['', Validators.required],
      observaciones: [''],
      lugar: [''],
      estado: [''],
      costoTotal: [''],
      duracionMinutos: ['',[Validators.required, Validators.pattern('^[0-9]+$')]],
      fechaHora: ['', Validators.required], // 🔥 nuevo
      activoId: [null, Validators.required],
      proveedorId: [null, Validators.required],
      valorHora: ['',[Validators.required, Validators.pattern('^([0-9]+|[0-9]{1,3}(\.[0-9]{3})*)(,[0-9]+)?$')]],
      horasEstimadas: ['',[Validators.required, Validators.pattern(/^\d+([.,]\d+)?$/)]],
      horas: [''],
      costoManoObraEstimada: [''],
      costoManoObra: [''],
      tipoMantenimiento: [null, Validators.required],
      repuestos: this.fb.array([]),
      // 🔥 Ingreso retroactivo: al marcarlo, "Fecha"/"Duración (min)"
      // dejan de ser obligatorias y en su lugar se piden el inicio y
      // termino REALES del trabajo (ver onToggleIngresoRetroactivo).
      ingresoRetroactivo: [false],
      fechaEjecucionReal: [''],
      fechaFinEjecucionReal: ['']
    });

    this.repuestoForm = this.fb.group({

      repuestoId: [null, Validators.required],
      cantidad: [1, [Validators.required, Validators.min(1)]]
    });

    this.activoControl.valueChanges.subscribe(activo => {
      this.ordenMantencionForm.patchValue({
        activoId: activo?.id || null
      });
    });

    this.proveedorControl.valueChanges.subscribe(value => {
      const seleccionado = value && typeof value === 'object' ? value : null;
      this.ordenMantencionForm.patchValue({ proveedorId: seleccionado?.id || null });

      const search = (typeof value === 'string' ? value : value?.nombre || '').toLowerCase().trim();
      this.proveedoresFiltrados = !search
        ? this.proveedores
        : this.proveedores.filter(p => p.nombre.toLowerCase().includes(search));
    });

    // 🔥 Acá el valor ya es directamente el string ("PREVENTIVO"/etc.),
    // no un objeto con id/nombre, así que se sincroniza tal cual.
    this.tipoMantenimientoControl.valueChanges.subscribe(value => {
      this.ordenMantencionForm.patchValue({ tipoMantenimiento: value || null });

      const search = (value || '').toLowerCase().trim();
      this.tiposMantenimientoFiltrados = !search
        ? this.tiposMantenimiento
        : this.tiposMantenimiento.filter(t => t.toLowerCase().includes(search));
    });

    this.ordenMantencionForm.get('duracionMinutos')?.valueChanges
      .subscribe(valor => {

        if (!['COMPLETADA', 'CANCELADA', 'EN_EJECUCION']
              .includes(this.estadoOrden)) {

          this.transformarAHora(); // 👈 lo que quieras ejecutar
          this.calcularCostoManoObra();
        }
      });

    this.ordenMantencionForm.get('valorHora')?.valueChanges
      .subscribe(valor => {

        if (this.ordenMantencionForm.get('ingresoRetroactivo')?.value) {
          this.calcularHorasReales();
        } else {
          this.calcularCostoManoObra();
        }
      });

    // 🔥 Ingreso retroactivo: al fijar el inicio y/o termino real del
    // trabajo, se calculan automaticamente las horas (estimadas y
    // reales, que en este caso son la misma) y el costo de mano de
    // obra (estimado y real) en base a esa duracion, sin pedirlos
    // aparte.
    this.ordenMantencionForm.get('fechaEjecucionReal')?.valueChanges
      .subscribe(() => this.calcularHorasReales());

    this.ordenMantencionForm.get('fechaFinEjecucionReal')?.valueChanges
      .subscribe(() => this.calcularHorasReales());

    // 🔥 lo mismo que ya pasa al pinchar/arrastrar en el calendario o
    // usar el FAB de mobile, pero para cuando el usuario escribe la
    // fecha directamente en el campo "Fecha" del formulario de
    // creacion: al tipear/elegir una fecha pasada, se resuelve al
    // toque (sin esperar a guardar) — mismo formulario reactivo tanto
    // en desktop como en mobile, asi que esto cubre ambos.
    this.ordenMantencionForm.get('fechaHora')?.valueChanges
      .subscribe(valor => this.onFechaHoraIngresada(valor));

    // 🔥 una vez que ya se cambio a la vista de ingreso retroactivo
    // (Inicio real / Termino real), el campo que queda visible y
    // editable es "Inicio real", no "Fecha". Si ahi el usuario corrige
    // la fecha a algo que ya no corresponde a un ingreso retroactivo
    // (mas de 24h atras, o una fecha futura), hay que resolverlo igual
    // que arriba en vez de dejarlo pasar en silencio hasta guardar.
    this.ordenMantencionForm.get('fechaEjecucionReal')?.valueChanges
      .subscribe(valor => this.onFechaEjecucionRealIngresada(valor));

    this.cargarEventos();
    this.cargarActivos();
    this.cargarRepustos();
    this.cargarProveedores();

  }



  onSelectRange(info: any) {

  const fecha = info.start;

  // 🔥 se permite crear ordenes hasta 24h atras (ingreso retroactivo,
  // ver puedeIngresoRetroactivo en el formulario): antes cualquier
  // fecha pasada, sin importar cuanto, quedaba bloqueada aqui mismo,
  // impidiendo siquiera abrir el modal.
  if (this.esFechaDemasiadoAntigua(fecha)) {
    Swal.fire({
      toast: true,
      position: 'top-end',
      icon: 'warning',
      title: 'No puedes usar fechas pasadas',
      timer: 2000,
      showConfirmButton: false
    });

    return;
  }

  // 🔥 fecha pasada (dentro de las 24h permitidas) pero el usuario no
  // tiene el rol para ingreso retroactivo: se bloquea aca mismo, con
  // el mismo mensaje que se usa al intentar arrastrar sin permiso, en
  // vez de abrir el modal igual y dejarla como una creacion normal.
  if (fecha.getTime() < Date.now() && !this.puedeIngresoRetroactivo) {
    Swal.fire({
      toast: true,
      position: 'top-end',
      icon: 'warning',
      title: 'No tienes permiso para ingresar ordenes de forma retroactiva',
      timer: 2500,
      showConfirmButton: false
    });

    return;
  }

  this.abrirModalCreacion(fecha);
  }

  formatearMiles(event: any, controlName: string): void {

    let valor = event.target.value;

    valor = valor.replace(/\./g, '');
    valor = valor.replace(/\D/g, '');

    this.ordenMantencionForm
      .get(controlName)
      ?.setValue(valor);

    event.target.value = Number(valor || 0)
      .toLocaleString('es-CL');
  }

  transformarAHora(){
    const horas = (this.ordenMantencionForm.get('duracionMinutos')?.value || 0) / 60;

    this.ordenMantencionForm.patchValue({
      horasEstimadas: horas
    });
  }

  calcularCostoManoObra(){
    let valorHora = String(this.ordenMantencionForm.get('valorHora')?.value || 0);
    valorHora = valorHora.replace(/\./g, '');
    valorHora = valorHora.replace(/\D/g, '');
    const horaEstimada = (this.ordenMantencionForm.get('horasEstimadas')?.value || 0);
    const costoManoObraEstimada = Number(valorHora) * horaEstimada;

    this.ordenMantencionForm.patchValue({
      costoManoObraEstimada: this.formatearMilesBlock(costoManoObraEstimada)
    });
  }

  // 🔥 hora estimada = duracion ESTIMADA (segundos) / 3600. Se usa al
  // mostrar una orden existente, para que "Horas estimadas" salga
  // siempre de la duracion planificada registrada en el backend
  // (duracionEstimadaSegundos), no de un campo aparte que pudiera
  // quedar desactualizado.
  private segundosAHoras(segundos: number | null | undefined): number | null {
    if (segundos === null || segundos === undefined) {
      return null;
    }

    return Math.round((segundos / 3600) * 100) / 100;
  }

  // 🔥 Ingreso retroactivo: el trabajo ya se realizo, asi que "horas
  // estimadas" y "horas reales" (y sus costos) son la misma cifra,
  // calculada a partir del inicio/termino real informado.
  calcularHorasReales(): void {

    if (!this.ordenMantencionForm.get('ingresoRetroactivo')?.value) {
      return;
    }

    const inicio = this.ordenMantencionForm.get('fechaEjecucionReal')?.value;
    const fin = this.ordenMantencionForm.get('fechaFinEjecucionReal')?.value;

    if (!inicio || !fin) {
      return;
    }

    const inicioMs = new Date(inicio).getTime();
    const finMs = new Date(fin).getTime();
    const horas = (finMs - inicioMs) / 3600000;

    if (!isFinite(horas) || horas <= 0) {
      return;
    }

    const horasRedondeadas = Math.round(horas * 100) / 100;

    let valorHora = String(this.ordenMantencionForm.get('valorHora')?.value || 0);
    valorHora = valorHora.replace(/\./g, '');
    valorHora = valorHora.replace(/\D/g, '');
    const costoManoObra = Number(valorHora) * horasRedondeadas;

    // 🔥 se guarda el numero CRUDO (no formateado) en el form control:
    // el input de "Costo mano de obra" en el HTML ya le aplica
    // formatearMilesBlock() al leerlo para mostrarlo ([value]="..."),
    // asi que si aca se guardaba ya formateado ("1.300") se formateaba
    // DOS veces: Number("1.300") interpreta el punto como decimal
    // (no como separador de miles) y da 1.3 -> "1,3". Guardando el
    // numero tal cual, el formateo lo hace una sola vez el template.
    const patchReales: any = {
      horas: horasRedondeadas,
      costoManoObra: costoManoObra
    };

    // 🔥 "estimado = real" solo aplica cuando se esta CREANDO una
    // orden retroactiva nueva (modoEdicion=false): ahi no existe una
    // estimacion propia distinta, asi que se igualan (asi el campo
    // "Horas estimadas", que es readonly, se va reflejando en vivo
    // mientras se completan las fechas de inicio/termino real). Si
    // se esta editando/viendo una orden que YA existia (modoEdicion=true),
    // su horasEstimadas/costoManoObraEstimada original (calculado en
    // base a su propia duracion planificada) NO debe pisarse con lo
    // calculado desde las horas reales.
    if (!this.modoEdicion) {
      patchReales.horasEstimadas = horasRedondeadas;
      patchReales.costoManoObraEstimada = this.formatearMilesBlock(costoManoObra);
    }

    this.ordenMantencionForm.patchValue(patchReales);
  }

  cargarProveedores() {
      this.proveedorService.getAll(this.page, this.size).subscribe({
        next: (data) => {
          this.proveedores = data.content;
          this.proveedoresFiltrados = data.content;

          //console.log("DATA:", this.proveedores)
        },
        error: (err) => {
          Swal.fire({
            icon: 'error',
            title: 'Error',
            text: err.error?.error || 'Error desconocido'
          });

          console.log("ERROR:", err);
        }
      });
    }

  // 🔥 Filtro empresa (solo SUPER_ADMIN): carga el combo de empresas y
  // recarga el calendario cuando se selecciona una (o se borra el
  // filtro), igual que en Dashboard/Reportes/Informe de Mantenciones.
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

  initFiltroEmpresa(): void {
    this.filtroEmpresaControl.valueChanges.subscribe(value => {
      const esObjeto = value && typeof value === 'object';
      const search = (esObjeto ? value.nombre : value || '').toLowerCase().trim();

      this.empresasFiltradas = !search
        ? this.empresas
        : this.empresas.filter(e => e.nombre.toLowerCase().includes(search));

      if (esObjeto) {
        this.filtroEmpresaId = value.id;
        this.cargarEventos();
      } else if (!search && this.filtroEmpresaId !== null) {
        this.filtroEmpresaId = null;
        this.cargarEventos();
      }
    });
  }

  // 🔥 CARGAR EVENTOS (solo actualiza events)
  cargarEventos() {
    this.ordenMantencionService.listar(this.filtroEmpresaId ?? undefined).subscribe((ordenesMantencion) => {

      const eventos = ordenesMantencion || [];

      const eventosMapeados = eventos.map(ordenMantencion => ({
        id: ordenMantencion.id?.toString(),
        title: ordenMantencion.titulo,
        start: ordenMantencion.fechaProgramada,
        end: ordenMantencion.fechaTermino,

        // 🎨 color dinámico
        backgroundColor: this.getColorPorEstado(ordenMantencion.estado),
        borderColor: this.getColorPorEstado(ordenMantencion.estado),
        textColor: '#fff',

        extendedProps: {
          estado: ordenMantencion.estado,
          tipoMantenimiento: ordenMantencion.tipoMantenimiento,
          duracionMinutos: ordenMantencion.duracionMinutos,
          observaciones: ordenMantencion.observaciones,
          costoTotal: ordenMantencion.costoTotal,
          activoId: ordenMantencion.activoId,
          usuarioId: ordenMantencion.usuarioId,
          proveedorId: ordenMantencion.proveedorId,
          valorHora: ordenMantencion.valorHora,
          horasEstimadas: ordenMantencion.horasEstimadas,
          horasReal: ordenMantencion.horasReal,
          costoManoObraEstimada: ordenMantencion.costoManoObraEstimada,
          costoManoObra: ordenMantencion.costoManoObra,
          repuestos: ordenMantencion.repuestos,
          tieneChecklist: ordenMantencion.tieneChecklist,
          fechaEjecucion: ordenMantencion.fechaEjecucion,
          fechaFinEjecucion: ordenMantencion.fechaFinEjecucion,
          duracionEstimadaSegundos: ordenMantencion.duracionEstimadaSegundos
        }
      }));

      // 🔥 El tooltip de cada evento se arma "al vuelo" en eventDidMount,
      // con el estado/tipo/duración de ese momento quemados en el HTML
      // del globo. eventDidMount solo se dispara cuando FullCalendar
      // MONTA un bloque por primera vez — si solo reasignamos
      // calendarOptions.events con datos nuevos, FullCalendar puede
      // reconocer que ya existe un evento con ese mismo id y actualizarlo
      // "in place" sin volver a montarlo, dejando el tooltip viejo (con
      // el estado anterior) pegado en pantalla aunque la orden ya haya
      // cambiado de estado (iniciar/detener/cancelar).
      //
      // Por eso, cuando el calendario ya está inicializado, se sacan
      // TODOS los eventos y se vuelven a agregar desde cero: así se
      // fuerza a que cada bloque (y su tooltip) se vuelva a montar con
      // los datos actuales, sin necesidad de recargar la página.
      const api = this.calendarComponent?.getApi();

      if (api) {
        api.removeAllEvents();
        api.addEventSource(eventosMapeados);
      } else {
        this.calendarOptions.events = eventosMapeados;
      }
    });

  }

  // 🔥 CARGAR ACTIVOS
  cargarActivos() {
    this.activoService.getAll(this.page, this.size).subscribe({
      next: (data) => {
        this.activos = data.content;

        // 🔥 IMPORTANTE: inicializar filtro cuando ya tienes datos
        this.initFiltroActivos();

        // 🔥 CLAVE: dispara el autocomplete
        this.activoControl.setValue('');

      },
      error: () => {
        console.log("error");
      }
    });
  }

  // 🔥 CARGAR REPUESTOS
  cargarRepustos(){
    this.repuestoService.getAll(this.page, this.size).subscribe({
      next: (data) => {

        if (!data) {
          return;
        }
        this.repuestos = data.content;

        // 🔥 IMPORTANTE: inicializar filtro cuando ya tienes datos
        //this.initFiltroActivos();

        // 🔥 CLAVE: dispara el autocomplete
        //this.activoControl.setValue('');

      },
      error: () => {
        console.log("error");
      }
    });
  }

  // 🟢 CLICK EN CALENDARIO
  onDateClick(info: any) {
    const fecha = info.date;

    // 🔥 se permite crear ordenes hasta 24h atras (ingreso retroactivo).
    if (this.esFechaDemasiadoAntigua(fecha)) {
      Swal.fire({
        toast: true,
        position: 'top-end',
        icon: 'warning',
        title: 'No puedes usar fechas pasadas',
        showConfirmButton: false,
        timer: 2500
      });
      return;
    }

    // 🔥 fecha pasada (dentro de las 24h permitidas) pero el usuario no
    // tiene el rol para ingreso retroactivo: se bloquea aca mismo, con
    // el mismo mensaje que se usa al intentar arrastrar sin permiso, en
    // vez de abrir el modal igual y dejarla como una creacion normal.
    if (fecha.getTime() < Date.now() && !this.puedeIngresoRetroactivo) {
      Swal.fire({
        toast: true,
        position: 'top-end',
        icon: 'warning',
        title: 'No tienes permiso para ingresar ordenes de forma retroactiva',
        showConfirmButton: false,
        timer: 2500
      });
      return;
    }

     const fechaLocal = this.formatFechaLocal(fecha);


    this.fechaSeleccionada = fechaLocal;
    this.modoEdicion = false;
    this.aplicarEstadoFormulario();

    this.limpiarFormulario();
    // 🔥 LIMPIAR AUTOCOMPLETE
    this.activoControl.reset();

    this.ordenMantencionForm.patchValue({
      fechaHora: fechaLocal
     });

    // 🔥 si se pincho dentro de las ultimas 24h, se asume ingreso
    // retroactivo y se marca el checkbox solo.
    this.aplicarIngresoRetroactivoSiCorresponde(fecha);

    this.repuestosAsociados = [];

    this.mostrarModal = true;
  }

  // 🟡 MOVER EVENTO
  onEventDrop(info: any) {
    if (!info.event.start) return;

    const id = info.event.id;
    // 🔥 clonar la fecha: info.revert() puede mutar el Date interno del
    // evento, y esta variable se sigue usando despues de revertir para
    // precargar el ingreso retroactivo.
    const nuevaFecha = new Date(info.event.start.getTime());

    const ahora = new Date();

    this.estadoOrden = info.event.extendedProps?.estado;

    // 🔥 solo se puede arrastrar (reprogramar a futuro o ingreso
    // retroactivo hacia el pasado) una orden PENDIENTE o PROGRAMADA;
    // el resto de los estados (EN_EJECUCION, PRE_COMPLETADA,
    // COMPLETADA, CANCELADA, ATRASADA) quedan bloqueados por completo.
    if (!['PENDIENTE', 'PROGRAMADA'].includes(this.estadoOrden)) {
      info.revert();
      Swal.fire({
        toast: true,
        position: 'top-end',
        icon: 'warning',
        title: 'No puedes arrastrar esta orden',
        showConfirmButton: false,
        timer: 2500
      });
      return;
    }

    // 🔥 mas de 24h atras: sigue bloqueado igual que antes
    if (this.esFechaDemasiadoAntigua(nuevaFecha)) {
      info.revert();
      Swal.fire({
        toast: true,
        position: 'top-end',
        icon: 'warning',
        title: 'No puedes usar fechas pasadas',
        showConfirmButton: false,
        timer: 2500
      });
      return;
    }

    // 🔥 fecha dentro de las ultimas 24h: en vez de "reprogramar", se
    // trata directamente como ingreso retroactivo (la orden ya se
    // realizo) — sin pedir confirmacion, igual que si el ingreso
    // retroactivo ya estuviera marcado.
    if (nuevaFecha.getTime() < ahora.getTime()) {

      if (!this.puedeIngresoRetroactivo) {
        info.revert();
        Swal.fire({
          toast: true,
          position: 'top-end',
          icon: 'warning',
          title: 'No tienes permiso para ingresar ordenes de forma retroactiva',
          showConfirmButton: false,
          timer: 2500
        });
        return;
      }

      this.guardarIngresoRetroactivoDesdeDrag(info, nuevaFecha);
      return;
    }

    Swal.fire({
      title: 'Reprogramar orden',
      input: 'textarea',
      inputLabel: 'Motivo de la reprogramación',
      inputPlaceholder: 'Escribe el motivo...',
      inputAttributes: {
        'aria-label': 'Motivo'
      },
      showCancelButton: true,
      confirmButtonText: 'Guardar',
      cancelButtonText: 'Cancelar',
      confirmButtonColor: '#3b82f6',
      cancelButtonColor: '#64748b',
      inputValidator: (value) => {
        if (!value || value.trim().length < 3) {
          return 'Debes ingresar un motivo válido';
        }
        return null;
      }
    }).then((result) => {

      // ❌ Canceló → volver atrás
      if (!result.isConfirmed) {
        info.revert();
        return;
      }

      const motivo = result.value;

      // ✅ Llamar backend
      this.ordenMantencionService.reprogramar(id, nuevaFecha, motivo)
        .subscribe({
          next: () => {
            Swal.fire({
              icon: 'success',
              title: 'Reprogramado',
              text: 'La orden fue actualizada',
              timer: 1500,
              showConfirmButton: false
            });
          },
          error: () => {
            info.revert();

            Swal.fire({
              icon: 'error',
              title: 'Error',
              text: 'No se pudo reprogramar'
            });
          }
        });

    });
  }

  // 🔵 CLICK EN EVENTO (EDITAR)
  onEventClick(info: any) {

    const fecha = info.event.start;
    if (!fecha) return;

    const activoId = info.event.extendedProps?.activoId;

    this.costoTotal = info.event.extendedProps?.costoTotal;

    if (activoId) {
      this.cargarRiesgo(activoId);
    }

     const fechaLocal = this.formatFechaLocal(fecha);

    this.estadoOrden = info.event.extendedProps?.estado;
    this.tieneChecklist = !!info.event.extendedProps?.tieneChecklist;

    this.fechaSeleccionada = fechaLocal;
    this.eventoSeleccionadoId = Number(info.event.id);
    this.modoEdicion = true;

    // 🔥 LIMPIAR FORMARRAY
    this.repuestosFormArray.clear();

    // 🔥 TOMAR REPUESTOS
    const repuestos =
      info.event.extendedProps?.repuestos || [];

    // 🔥 CARGAR REPUESTOS AL FORMARRAY
    repuestos.forEach((r: any) => {

      const repuestoGroup = this.fb.group({
        repuestoId: [r.repuestoId],
        cantidad: [r.cantidad]
      });

      this.repuestosFormArray.push(repuestoGroup);
    });

    this.ordenMantencionForm.patchValue({
      titulo: info.event.title,
      observaciones: info.event.extendedProps?.observaciones || '',
      estado: this.estadoOrden,
      duracionMinutos: info.event.extendedProps?.duracionMinutos || '',
      tipoMantenimiento: info.event.extendedProps?.tipoMantenimiento || '',
      proveedorId: info.event.extendedProps?.proveedorId || '',
      costoTotal: info.event.extendedProps?.costoTotal || '',
      valorHora: this.formatearMilesBlock(info.event.extendedProps?.valorHora) || '',
      horasEstimadas: info.event.extendedProps?.horasEstimadas,
      horas: info.event.extendedProps?.horasReal || '',
      costoManoObraEstimada: info.event.extendedProps?.costoManoObraEstimada || '',
      costoManoObra: info.event.extendedProps?.costoManoObra || '',
      fechaHora: fechaLocal,
      repuestos: info.event.extendedProps?.repuestos || ''
    });

     this.repuestosAsociados = info.event.extendedProps?.repuestos ?? [];

    // 🔥 AQUÍ LA MAGIA
    this.setActivoSeleccionado(activoId);
    this.setProveedorSeleccionado(info.event.extendedProps?.proveedorId);
    this.tipoMantenimientoControl.setValue(info.event.extendedProps?.tipoMantenimiento || '');

    // 🔥 en PRE_COMPLETADA/COMPLETADA se muestra el horario REAL de
    // ejecucion (Inicio real / Termino real) en vez de la fecha
    // estimada de la programacion. emitEvent:false para no disparar
    // calcularHorasReales() y pisar horas/costoManoObra, que ya se
    // patchearon arriba con los valores exactos que calculo el backend.
    if (['PRE_COMPLETADA', 'COMPLETADA'].includes(this.estadoOrden)) {
      const fechaEjecucion = info.event.extendedProps?.fechaEjecucion;
      const fechaFinEjecucion = info.event.extendedProps?.fechaFinEjecucion;

      this.ordenMantencionForm.patchValue({
        ingresoRetroactivo: true,
        fechaEjecucionReal: fechaEjecucion ? this.formatFechaLocal(new Date(fechaEjecucion)) : '',
        fechaFinEjecucionReal: fechaFinEjecucion ? this.formatFechaLocal(new Date(fechaFinEjecucion)) : ''
      }, { emitEvent: false });
    } else {
      this.ordenMantencionForm.patchValue({ ingresoRetroactivo: false }, { emitEvent: false });
    }

    this.aplicarEstadoFormulario();

    this.mostrarModal = true;
  }

  // 🔥 Ingreso retroactivo al arrastrar una orden ya programada hasta
  // 24h atras: se guarda de inmediato como si el ingreso retroactivo
  // ya estuviera marcado (sin pedir confirmacion ni pasar por el
  // formulario), con inicio = punto donde se soltó el drag y termino =
  // inicio + la duracion en minutos que ya tenia la orden. Al terminar,
  // se reabre el modal ya como una orden PRE_COMPLETADA normal (con sus
  // botones habituales: Cancelar, Adjuntar/Validar checklist, Completar).
  private guardarIngresoRetroactivoDesdeDrag(info: any, fechaInicio: Date): void {

    const id = Number(info.event.id);
    const duracionMinutos = Number(info.event.extendedProps?.duracionMinutos) || 0;
    const fechaFin = new Date(fechaInicio.getTime() + duracionMinutos * 60000);

    const repuestos = (info.event.extendedProps?.repuestos || []).map((r: any) => ({
      repuestoId: r.repuestoId,
      cantidad: r.cantidad
    }));

    const data: any = {
      titulo: info.event.title,
      tipoMantenimiento: info.event.extendedProps?.tipoMantenimiento,
      observaciones: info.event.extendedProps?.observaciones,
      repuestos,
      ingresoRetroactivo: true,
      fechaEjecucionReal: this.formatFechaLocal(fechaInicio),
      fechaFinEjecucionReal: this.formatFechaLocal(fechaFin)
    };

    this.ordenMantencionService.actualizar(id, data).subscribe({
      next: () => {
        Swal.fire({
          toast: true,
          position: 'top-end',
          icon: 'success',
          title: 'Orden marcada como realizada (pendiente aprobación final)',
          showConfirmButton: false,
          timer: 2000
        });

        // 🔥 el arrastre queda resuelto con el toast de arriba: ya no
        // se reabre el modal automaticamente despues de guardar el
        // ingreso retroactivo.
        this.cargarEventos();
      },
      error: (err) => {
        info.revert();
        Swal.fire({
          icon: 'error',
          title: 'Error',
          text: err.error?.error || 'No se pudo registrar el ingreso retroactivo'
        });
      }
    });
  }

  private reabrirOrdenActualizada(id: number): void {
    const evento = this.calendarComponent?.getApi()?.getEventById(String(id));

    if (evento) {
      this.onEventClick({ event: evento });
    }
  }

  // 💾 GUARDAR (CREAR / EDITAR)
  guardar() {
    this.ordenMantencionForm.enable();
    if (!FormUtils.esValido(this.ordenMantencionForm)) {
      const campo = FormUtils.getPrimerCampoInvalido(this.ordenMantencionForm);
      FormUtils.marcarComoTocados(this.ordenMantencionForm);
      Swal.fire({
        icon: 'warning',
        title: 'Formulario incompleto',
        text: `Revisa el campo: ${campo}`
      });

      console.log(FormUtils.getErrores(this.ordenMantencionForm));

      return;
    }

    const { titulo, observaciones, activoId, proveedorId, tipoMantenimiento, duracionMinutos,
            fechaHora, valorHora, horasEstimadas, costoManoObraEstimada,
            ingresoRetroactivo, fechaEjecucionReal, fechaFinEjecucionReal } = this.ordenMantencionForm.getRawValue();

    // 🔥 Ingreso retroactivo aplica tanto al CREAR una orden nueva como
    // al EDITAR una existente (p.ej. al arrastrarla en el calendario
    // hasta 24h atras): la orden queda PRE_COMPLETADA con el tiempo
    // real declarado, en vez de PROGRAMADA con fecha/duracion estimadas.
    const esRetroactivo = !!ingresoRetroactivo;

    const data: any = {
      titulo,
      tipoMantenimiento,
      observaciones,
      activoId,
      proveedorId,
      usuarioId: this.usuario.sub,
      planMantenimientoId: "1",
      valorHora: valorHora.replace(".", ""),
      horasEstimadas,
      costoManoObraEstimada: costoManoObraEstimada.replace(".", ""),
      // 🔥 NUEVO
      repuestos: this.repuestosFormArray.getRawValue()
    };

    if (esRetroactivo) {
      data.ingresoRetroactivo = true;
      data.fechaEjecucionReal = fechaEjecucionReal;
      data.fechaFinEjecucionReal = fechaFinEjecucionReal;
    } else {
      data.fechaProgramada = fechaHora;
      data.duracionMinutos = duracionMinutos;
      data.estado = "PROGRAMADA";
    }

    if (this.modoEdicion) {
      // 🔵 EDITAR
      this.ordenMantencionService
      .actualizar(this.eventoSeleccionadoId, data)
      .subscribe({
        next: () => {
          this.cargarEventos();
          this.cerrar();
        },
        error: (err) => {
          Swal.fire({
            icon: 'error',
            title: 'Error',
            text: err.error?.error || 'Error desconocido'
          });
        }
      });
    } else {
      const activoSeleccionado = this.activoControl.value;

      this.ordenMantencionForm.patchValue({
        activoId: activoSeleccionado?.id || null
      });

      // 🟢 CREAR
      this.ordenMantencionService.crear(data)
      .subscribe({
        next: () => {
          this.repuestosFormArray.clear();
          this.cargarEventos();
          this.cerrar();
        },
        error: (err) => {
          Swal.fire({
            icon: 'error',
            title: 'Error',
            text: err.error?.error || 'Error desconocido'
          });
        }
      });
    }
  }

  private abrirModalCreacion(fecha: Date) {

    this.limpiarFormulario();
    this.activoControl.reset();

    this.ordenMantencionForm.patchValue({
      fechaHora: this.formatFechaLocal(fecha)
    });

    // 🔥 si se pincho dentro de las ultimas 24h, se asume ingreso
    // retroactivo y se marca el checkbox solo (ver esFechaDemasiadoAntigua:
    // mas de 24h atras ya ni siquiera llega a abrir el modal).
    this.aplicarIngresoRetroactivoSiCorresponde(fecha);

    this.modoEdicion = false;
    this.mostrarModal = true;

    setTimeout(() => {
      this.calendarComponent?.getApi()?.updateSize();
    }, 100);
  }

  // 🔥 se dispara con cada cambio del campo "Fecha" (tipeado a mano o
  // elegido con el selector nativo, en desktop o mobile) MIENTRAS se
  // esta CREANDO una orden nueva (no aplica editando una existente).
  // Si la fecha ingresada ya paso:
  //   - dentro de las 24h permitidas y el usuario tiene el rol
  //     necesario -> se pasa solo a la vista de ingreso retroactivo
  //     (Inicio real / Termino real), igual que al pinchar/arrastrar.
  //   - dentro de las 24h pero SIN el rol -> mensaje "no tienes
  //     permisos para crear ordenes retroactivas" y se limpia el campo.
  //   - mas de 24h atras (para cualquiera, incluso con el rol) ->
  //     mensaje "no se pueden crear ordenes con fechas pasadas" y se
  //     limpia el campo.
  private onFechaHoraIngresada(valor: string): void {

    if (this.modoEdicion || !valor) {
      return;
    }

    if (this.ordenMantencionForm.get('ingresoRetroactivo')?.value) {
      return;
    }

    const fecha = new Date(valor);

    if (isNaN(fecha.getTime()) || fecha.getTime() >= Date.now()) {
      return;
    }

    if (this.esFechaDemasiadoAntigua(fecha)) {
      Swal.fire({
        toast: true,
        position: 'top-end',
        icon: 'warning',
        title: 'No se pueden crear ordenes con fechas pasadas',
        showConfirmButton: false,
        timer: 2500
      });
      this.ordenMantencionForm.get('fechaHora')?.setValue('', { emitEvent: false });
      return;
    }

    if (!this.puedeIngresoRetroactivo) {
      Swal.fire({
        toast: true,
        position: 'top-end',
        icon: 'warning',
        title: 'No tienes permisos para crear ordenes retroactivas',
        showConfirmButton: false,
        timer: 2500
      });
      this.ordenMantencionForm.get('fechaHora')?.setValue('', { emitEvent: false });
      return;
    }

    this.aplicarIngresoRetroactivoSiCorresponde(fecha);
  }

  // 🔥 mientras se esta CREANDO una orden nueva y ya se paso a la
  // vista de ingreso retroactivo (Inicio real / Termino real), este
  // handler vigila cambios posteriores al campo "Inicio real":
  //   - si lo corrigen a una fecha futura (o "ahora") -> ya no
  //     corresponde ingreso retroactivo, se vuelve a la vista normal
  //     (Fecha / Duracion), llevandose la fecha escrita.
  //   - si lo corrigen a algo mas de 24h atras -> mensaje "no se
  //     pueden crear ordenes con fechas pasadas" y se limpia el campo
  //     (el permiso de rol ya se valido al entrar en modo retroactivo,
  //     no hace falta volver a chequearlo aca).
  private onFechaEjecucionRealIngresada(valor: string): void {

    if (this.modoEdicion || !valor) {
      return;
    }

    if (!this.ordenMantencionForm.get('ingresoRetroactivo')?.value) {
      return;
    }

    const fecha = new Date(valor);

    if (isNaN(fecha.getTime())) {
      return;
    }

    if (fecha.getTime() >= Date.now()) {
      this.ordenMantencionForm.patchValue({
        ingresoRetroactivo: false,
        fechaHora: valor
      });
      this.onToggleIngresoRetroactivo();
      return;
    }

    if (this.esFechaDemasiadoAntigua(fecha)) {
      Swal.fire({
        toast: true,
        position: 'top-end',
        icon: 'warning',
        title: 'No se pueden crear ordenes con fechas pasadas',
        showConfirmButton: false,
        timer: 2500
      });
      this.ordenMantencionForm.get('fechaEjecucionReal')?.setValue('', { emitEvent: false });
      return;
    }
  }

  // 🔒 Ingreso retroactivo: si la fecha clickeada ya paso (pero esta
  // dentro de la ventana de 24h permitida) y el usuario tiene el rol
  // necesario, se marca el checkbox y se precarga el inicio real con
  // esa misma fecha/hora.
  private aplicarIngresoRetroactivoSiCorresponde(fecha: Date): void {

    const yaPaso = fecha.getTime() < Date.now();

    if (!this.puedeIngresoRetroactivo || !yaPaso) {
      return;
    }

    this.ordenMantencionForm.patchValue({
      ingresoRetroactivo: true,
      fechaEjecucionReal: this.formatFechaLocal(fecha)
    });

    this.onToggleIngresoRetroactivo();
  }

  // ❌ CANCELAR ORDEN
  cancelarOrden() {
    Swal.fire({
      title: 'Cancelar orden',
      input: 'textarea',
      inputLabel: 'Motivo',
      inputPlaceholder: 'Escribe el motivo...',
      showCancelButton: true,
      confirmButtonText: 'Cancelar orden',
      confirmButtonColor: '#ef4444'
    }).then(result => {

      if (!result.isConfirmed) return;

      const motivo = result.value;

      this.ordenMantencionService.cancelar(
        this.eventoSeleccionadoId,
        motivo,
        this.usuario.sub
      ).subscribe(() => {
        this.cargarEventos();
        this.cerrar();

        Swal.fire({
          icon: 'success',
          title: 'Orden cancelada',
          timer: 1500,
          showConfirmButton: false
        });
      });

    });
  }

  cerrar() {
    this.mostrarModal = false;
    this.modoEdicion = false;
    this.estadoOrden = '';
    this.tieneChecklist = false;

    setTimeout(() => {
      this.calendarComponent?.getApi()?.updateSize();
    }, 200);
  }



  getColorPorEstado(estado?: string): string {
    switch (estado) {
      case 'PENDIENTE':
        return '#eab308'; // amarillo

      case 'PROGRAMADA':
        return '#3b82f6'; // azul

      case 'EN_EJECUCION':
        return '#f97316'; // naranja

      case 'PRE_COMPLETADA':
        return '#14b8a6'; // teal

      case 'COMPLETADA':
        return '#22c55e'; // verde

      case 'CANCELADA':
        return '#ef4444'; // rojo

      default:
        return '#6b7280'; // gris
    }
  }

  obtenerClaseEstado(estado?: string): string {

    switch (estado) {

      case 'PENDIENTE':
        return 'secondary';

      case 'PROGRAMADA':
        return 'primary';

      case 'EN_EJECUCION':
        return 'danger';

      case 'PRE_COMPLETADA':
        return 'warning';

      case 'COMPLETADA':
        return 'success';

      case 'CANCELADA':
        return 'dark';

      default:
        return 'success';
    }
  }

  obtenerTextoBoton(estado?: string): string {

    switch (estado) {

      case 'EN_EJECUCION':
        return '⏹ Terminar';

      case 'PRE_COMPLETADA':
        return '✔ Completar';

      case 'COMPLETADA':
        return '✓ Completada';

      case 'CANCELADA':
        return '✖ Cancelada';

      default:
        return '▶ Iniciar';
    }
  }

  get isReadOnly(): boolean {
    // 🔥 al editar una orden ya creada, los campos generales (título,
    // observaciones, proveedor, valor hora, etc.) quedan de solo
    // lectura: ya no existe el botón "Actualizar" para guardarlos acá.
    // Los repuestos se agregan/eliminan aparte y persisten al instante;
    // el resto de acciones (iniciar/terminar/cancelar/checklist) tienen
    // sus propios botones independientes de este formulario.
    return this.modoEdicion;
  }

  ngOnChanges() {
    this.aplicarEstadoFormulario();
  }

  toggleMantencion() {
    const id = this.eventoSeleccionadoId;
    if (this.estadoOrden === 'EN_EJECUCION') {
      this.confirmarPreDetencionConArchivo(id);
    } else if (this.estadoOrden === 'PRE_COMPLETADA') {
      if (!this.tieneChecklist) {
        this.confirmarCompletarSinChecklist(id);
      } else {
        this.detenerMantencion(id);
      }
    } else {
      this.iniciarMantencion(id);
    }

  }

  // 🔥 al completar sin haber adjuntado el checklist se advierte antes
  // de continuar: una vez COMPLETADA la orden ya no admite adjuntar
  // nada (ver subirChecklist en el backend).
  confirmarCompletarSinChecklist(id: number) {
    Swal.fire({
      icon: 'warning',
      title: 'Falta el checklist',
      text: 'Esta orden no tiene un checklist adjunto. Una vez completada ya no podrá adjuntarlo. ¿Desea completarla de todas formas?',
      showCancelButton: true,
      confirmButtonText: 'Sí, completar',
      cancelButtonText: 'Cancelar',
      confirmButtonColor: '#3b82f6',
      cancelButtonColor: '#64748b'
    }).then((result) => {
      if (result.isConfirmed) {
        this.detenerMantencion(id);
      }
    });
  }

  iniciarMantencion(id: number) {
    // 🔥 aquí llamas backend
    this.ordenMantencionService.iniciar(id).subscribe({
      next: (orden) => {
        this.estadoOrden = orden.estado; // o COMPLETADA si aplica
        console.log('Iniciado OK');
        this.cargarEventos();
        this.cerrar();
      },
      error: (err) => {
        Swal.fire({
          toast: true,
          position: 'top-end',
          icon: 'error',
          title: `No se pudo iniciar: ${err.error.error}`,
          showConfirmButton: false,
          timer: 2500
        });
      }
    });
  }

  detenerMantencion(id: number) {
    // 🔥 backend
    this.ordenMantencionService.detener(id).subscribe({
      next: (orden) => {
        this.estadoOrden = orden.estado; // o COMPLETADA si aplica
        console.log('Detenido OK');
        this.cargarEventos();
        this.cerrar();
      },
      error: (err) => {
        // 🔥 antes se mostraba un mensaje generico fijo, ignorando el
        // motivo real que informa el backend (por ejemplo, sin permiso:
        // ver CustomAccessDeniedHandler, "No tienes permisos para
        // acceder a este recurso"). Igual que en iniciarMantencion().
        Swal.fire({
          toast: true,
          position: 'top-end',
          icon: 'error',
          title: `No se pudo completar: ${err.error?.error || 'Intenta nuevamente'}`,
          showConfirmButton: false,
          timer: 3000
        });
      }
    });
  }

  detenerMantencionConArchivo(id: number, archivo: File | null) {

    const formData = new FormData();
    if (archivo) {
      formData.append('archivo', archivo);
    }

    this.ordenMantencionService.detenerConArchivo(id, formData)
      .subscribe({
        next: () => {
          this.estadoOrden = 'PRE_COMPLETADA';
          this.tieneChecklist = !!archivo;

          console.log('Detenido OK');
          this.cargarEventos();
          this.cerrar();

          if (archivo) {
            Swal.fire({
              icon: 'success',
              title: 'Mantención detenida',
              timer: 1500,
              showConfirmButton: false
            });
          } else {
            // 🔥 no se adjuntó checklist al terminar: se le avisa al
            // usuario que tiene 24h para ingresarlo (o un repuesto/
            // fungible) antes de que el supervisor complete la orden.
            Swal.fire({
              icon: 'info',
              title: 'Orden pre completada',
              html: `
                <p>
                  Tiene <b>24 horas</b> para ingresar el checklist de
                  mantención, o algún repuesto o fungible utilizado.
                </p>
                <p>
                  Puede hacerlo abriendo nuevamente esta orden, antes de
                  que el supervisor la dé por completada.
                </p>
              `,
              confirmButtonText: 'Entendido'
            });
          }
        },
        error: (err: { error: { error: any; }; }) => {
          Swal.fire({
            icon: 'error',
            title: err.error?.error || 'Error al detener'
          });
        }
      });
  }

  confirmarPreDetencionConArchivo(id: number) {
    Swal.fire({
      title: 'Pre Completar mantención',
      html: `
        <div style="text-align:left">
          <p>
            Está a punto de finalizar esta orden de mantención.
          </p>

          <ul style="margin-top:10px">
            <li>La orden quedará marcada como pre completada, hasta que el supervisor la dé por finalizada.</li>
            <li>Se registrará el documento de chequeo como respaldo, si lo adjunta.</li>
            <li>La información quedará disponible para futuras auditorías y consultas.</li>
          </ul>

          <p style="margin-top:15px">
            Puede adjuntar el documento de chequeo (checklist) ahora, o
            hacerlo después: tiene <b>24 horas</b> para ingresarlo, junto
            con los repuestos o fungibles utilizados.
          </p>
          <small style="color:#64748b">
            Formatos permitidos: PDF, JPG, JPEG y PNG.
          </small>

          <input type="file" id="fileInput" class="swal2-file"  accept=".pdf,.jpg,.jpeg,.png" />
      </div>
      `,
      confirmButtonText: 'Terminar',
      showCancelButton: true,
      cancelButtonText: 'Cancelar',
      preConfirm: () => {
        const input = document.getElementById('fileInput') as HTMLInputElement;

        // 🔥 el checklist ya no es obligatorio para terminar la orden.
        return input?.files?.length ? input.files[0] : null;
      }
    }).then(result => {

      if (!result.isConfirmed) return;

      this.detenerMantencionConArchivo(id, result.value);
    });
  }

  // 🔥 permite adjuntar el checklist DESPUÉS de haber terminado la orden
  // sin él (mientras siga PRE_COMPLETADA), dentro del plazo de 24h que
  // se avisó en el modal anterior.
  adjuntarChecklistPosterior(): void {
    const id = this.eventoSeleccionadoId;

    Swal.fire({
      title: 'Adjuntar checklist',
      html: `
        <div style="text-align:left">
          <p>Adjunte el documento de chequeo de mantención.</p>
          <small style="color:#64748b">
            Formatos permitidos: PDF, JPG, JPEG y PNG.
          </small>
          <input type="file" id="fileInputChecklist" class="swal2-file" accept=".pdf,.jpg,.jpeg,.png" />
        </div>
      `,
      confirmButtonText: 'Guardar',
      showCancelButton: true,
      cancelButtonText: 'Cancelar',
      preConfirm: () => {
        const input = document.getElementById('fileInputChecklist') as HTMLInputElement;

        if (!input?.files?.length) {
          Swal.showValidationMessage('Debes seleccionar un archivo');
          return false;
        }

        return input.files[0];
      }
    }).then(result => {

      if (!result.isConfirmed) return;

      const formData = new FormData();
      formData.append('archivo', result.value);

      this.ordenMantencionService.subirChecklist(id, formData)
        .subscribe({
          next: () => {
            this.tieneChecklist = true;

            Swal.fire({
              icon: 'success',
              title: 'Checklist adjuntado',
              timer: 1500,
              showConfirmButton: false
            });

            this.cargarEventos();
          },
          error: (err: { error: { error: any; }; }) => {
            Swal.fire({
              icon: 'error',
              title: err.error?.error || 'Error al adjuntar el checklist'
            });
          }
        });
    });
  }

  validarChecklist(): void {

    if (this.isMobile()) {
      // 🔥 en mobile se mantiene el visor dentro de un modal: imagen
      // inline o iframe para el resto de tipos de archivo (ej. PDF).
      this.ordenMantencionService
      .verArchivo(this.eventoSeleccionadoId)
      .subscribe({

        next: (blob) => {

          const url = URL.createObjectURL(blob);

          this.esImagenChecklist = blob.type.startsWith('image/');

          if (this.esImagenChecklist) {
            // <img src> exige un SafeUrl, no un SafeResourceUrl.
            this.archivoUrlImagen = this.sanitizer.bypassSecurityTrustUrl(url);
          } else {
            this.archivoUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
          }

          this.mostrarModalArchivo = true;
        },

        error: () => {
          Swal.fire(
            'Error',
            'No fue posible cargar el archivo',
            'error'
          );
        }

      });

      return;
    }

    // 🔥 en desktop se abre directo en una pestaña nueva, de forma
    // sincrónica dentro del click (con about:blank), para que el
    // navegador no la bloquee como popup. Una vez llega el archivo
    // (async) se navega esa misma pestaña al blob.
    const nuevaPestana = window.open('', '_blank');

    this.ordenMantencionService
    .verArchivo(this.eventoSeleccionadoId)
    .subscribe({

      next: (blob) => {

        const url = URL.createObjectURL(blob);

        if (nuevaPestana) {
          nuevaPestana.location.href = url;
        } else {
          // 🔥 respaldo por si igual quedó bloqueada (bloqueador estricto).
          window.open(url, '_blank');
        }
      },

      error: () => {
        if (nuevaPestana) {
          nuevaPestana.close();
        }

        Swal.fire(
          'Error',
          'No fue posible cargar el archivo',
          'error'
        );
      }

    });

  }

  cerrarModalArchivo(): void {
    this.mostrarModalArchivo = false;
  }

  get titulo(): string {
    if (this.estadoOrden === 'COMPLETADA') return 'Orden';
    if (this.modoEdicion) return 'Actualizar Orden';
    return 'Nueva Orden';
  }

  initFiltroActivos() {
    this.activosFiltrados = this.activos;

    this.activoControl.valueChanges.subscribe(value => {
      const search = (typeof value === 'string' ? value : value?.nombre || '')
        .toLowerCase()
        .trim();

      if (!search) {
        this.activosFiltrados = this.activos;
      } else {
        this.activosFiltrados = this.activos.filter(a =>
          a.nombre.toLowerCase().includes(search)
        );
      }
    });
  }

  displayActivo = (activo: any): string => {
    return activo?.nombre ?? '';
  };

  onFocusActivo() {
    // 🔥 fuerza a emitir para mostrar todos
    this.activosFiltrados = this.activos;
  }

  displayProveedor = (proveedor: any): string => proveedor?.nombre ?? '';

  onFocusProveedor() {
    this.proveedoresFiltrados = this.proveedores;
  }

  onFocusTipoMantenimiento() {
    this.tiposMantenimientoFiltrados = this.tiposMantenimiento;
  }

  setActivoSeleccionado(activoId: number) {

    if (!this.activos || this.activos.length === 0) {
      setTimeout(() => this.setActivoSeleccionado(activoId), 200);
      return;
    }

    const activo = this.activos.find(a => a.id === activoId);

    if (activo) {

      this.activosFiltrados = [...this.activos];

      setTimeout(() => {
        this.activoControl.setValue(activo);
      });

    }
  }

  setProveedorSeleccionado(proveedorId: number) {

    if (!this.proveedores || this.proveedores.length === 0) {
      setTimeout(() => this.setProveedorSeleccionado(proveedorId), 200);
      return;
    }

    const proveedor = this.proveedores.find(p => p.id === proveedorId);

    if (proveedor) {

      this.proveedoresFiltrados = [...this.proveedores];

      setTimeout(() => {
        this.proveedorControl.setValue(proveedor);
      });

    }
  }

  puedeEditar(): boolean {
    if (!this.modoEdicion) return true; // 🔥 nueva orden

    return this.estadoOrden === 'PENDIENTE' ||
          this.estadoOrden === 'PROGRAMADA';
  }

  aplicarEstadoFormulario() {
    if (!this.isReadOnly) {
      this.ordenMantencionForm.enable();
      this.activoControl.enable();
      this.proveedorControl.enable();
      this.tipoMantenimientoControl.enable();
    } else {
      this.ordenMantencionForm.disable();
      this.activoControl.disable();
      this.proveedorControl.disable();
      this.tipoMantenimientoControl.disable();
    }
  }

  cargarRiesgo(activoId: number) {
    this.ordenMantencionService.getRiesgo(activoId)
      .subscribe((res: any) => {
        this.riesgo = res.riesgo;
        this.nivel = res.nivel;
      });
  }

  agregarRepuesto(): void {
    const group = this.fb.group({
      repuestoId: [null, Validators.required],
      cantidad: [
        1,
        [
          Validators.required,
          Validators.min(1)
        ]
      ]
    });

    this.repuestosFormArray?.push(group);
  }

  get repuestosFormArray(): FormArray {
    return this.ordenMantencionForm.get('repuestos') as FormArray;
  }

  getRepuestoGroup(i: number): FormGroup {
    return this.repuestosFormArray.at(i) as FormGroup;
  }

  eliminarRepuesto(index: number): void {

    const item = this.repuestosAsociados[index];

    // 🔥 la orden YA EXISTE y este repuesto ya está guardado en la BD
    // (tiene id real): hay que borrarlo ahí también, reponiendo el stock.
    if (this.modoEdicion && item?.id) {
      this.ordenRepuestoService.eliminar(item.id).subscribe({
        next: () => {
          this.repuestosAsociados.splice(index, 1);
          this.cargarEventos();

          Swal.fire({
            toast: true,
            position: 'top-end',
            icon: 'success',
            title: 'Repuesto eliminado',
            showConfirmButton: false,
            timer: 1500
          });
        },
        error: (err: { error: { error: any; }; }) => {
          Swal.fire({
            icon: 'error',
            title: err.error?.error || 'No se pudo eliminar el repuesto'
          });
        }
      });
      return;
    }

    // 🔥 orden AÚN NO existe (creación): solo se maneja localmente,
    // se persiste recién al crear la orden.
    this.repuestosFormArray?.removeAt(index);

    // 🔥 mantener sincronizada la tabla "Repuestos o Fungibles"
    this.sincronizarRepuestosAsociados();
  }

  abrirModalRepuesto(): void {
    this.repuestoForm.reset({
      cantidad: 1

    });

    this.mostrarModalRepuesto = true;
  }

  cerrarModalRepuesto(): void {

    this.mostrarModalRepuesto = false;
  }

  confirmarAgregarRepuesto(): void {
    if (this.repuestoForm.invalid) {
      this.repuestoForm.markAllAsTouched();
      return;
    }

    const repuestoId =
      Number(this.repuestoForm.value.repuestoId);

    const cantidad =
      Number(this.repuestoForm.value.cantidad);

    const repuesto = this.repuestos.find(r => Number(r.id) === repuestoId);

    // 🔥 la orden YA EXISTE: se guarda de inmediato en la BD (descuenta
    // stock al instante), sin esperar a ningún botón "Actualizar".
    if (this.modoEdicion && this.eventoSeleccionadoId) {
      this.ordenRepuestoService.agregar({
        ordenId: this.eventoSeleccionadoId,
        repuestoId,
        cantidad,
        costoUnitario: Number(repuesto?.costoUnitario) || 0
      }).subscribe({
        next: (creado: any) => {
          // 🔥 el backend fusiona la cantidad en la misma fila si el
          // repuesto ya estaba agregado a esta orden (mismo id): hay que
          // reflejar esa actualización acá, no duplicar la fila.
          const filaActualizada = {
            id: creado.id,
            repuestoId: creado.repuestoId,
            repuestoNombre: creado.repuestoNombre || repuesto?.nombre || '',
            cantidad: creado.cantidad,
            costoUnitario: creado.costoUnitario,
            costoTotal: creado.costoTotal
          };

          const idx = this.repuestosAsociados.findIndex(
            (r: any) => Number(r.repuestoId) === Number(creado.repuestoId)
          );

          if (idx >= 0) {
            this.repuestosAsociados[idx] = filaActualizada;
          } else {
            this.repuestosAsociados.push(filaActualizada);
          }

          this.repuestoForm.reset({ repuestoId: null, cantidad: 1 });
          this.cerrarModalRepuesto();
          this.cargarEventos();

          Swal.fire({
            toast: true,
            position: 'top-end',
            icon: 'success',
            title: 'Repuesto agregado',
            showConfirmButton: false,
            timer: 1500
          });
        },
        error: (err: { error: { error: any; }; }) => {
          Swal.fire({
            icon: 'error',
            title: err.error?.error || 'No se pudo agregar el repuesto'
          });
        }
      });
      return;
    }

    // 🔥 orden AÚN NO existe (creación): se maneja localmente y se
    // persiste recién al crear la orden.

    // 🔍 buscar si ya existe
    const existente =
      this.repuestosFormArray?.controls.find(control =>
        Number(control.value.repuestoId) === repuestoId
      );

    // ✅ SI EXISTE → SUMAR CANTIDAD
    if (existente) {
      const cantidadActual =
        Number(existente.value.cantidad);

      existente.patchValue({
        cantidad: cantidadActual + cantidad
      });
    }
    // ✅ SI NO EXISTE → AGREGAR
    else {
      const grupo = this.fb.group({
        repuestoId: [repuestoId],
        cantidad: [cantidad]
      });
      this.repuestosFormArray?.push(grupo);
    }

    // 🔥 reflejar de inmediato en la tabla "Repuestos o Fungibles"
    // (repuestosAsociados), sin esperar a guardar/recargar la orden.
    this.sincronizarRepuestosAsociados();

    // 🔥 limpiar form
    this.repuestoForm.reset({
      repuestoId: null,
      cantidad: 1
    });

    this.cerrarModalRepuesto();
  }

  // 🔥 Reconstruye repuestosAsociados (tabla de solo lectura del modal
  // principal) a partir del carrito real (repuestosFormArray), buscando
  // nombre y costo unitario en el catálogo ya cargado (this.repuestos).
  // El costoTotal mostrado acá es una estimación con el precio actual del
  // catálogo; al guardar, el backend recalcula el valor real.
  private sincronizarRepuestosAsociados(): void {
    this.repuestosAsociados = this.repuestosFormArray.getRawValue().map((r: any) => {
      const repuesto = this.repuestos.find(x => Number(x.id) === Number(r.repuestoId));
      const costoUnitario = Number(repuesto?.costoUnitario) || 0;

      return {
        repuestoId: r.repuestoId,
        repuestoNombre: repuesto?.nombre || '',
        cantidad: r.cantidad,
        costoTotal: costoUnitario * Number(r.cantidad)
      };
    });
  }



  obtenerNombreRepuesto(id: number): string {
    const repuesto = this.repuestos.find(
      r => Number(r.id) === Number(id)
    );
    return repuesto?.nombre || '';
  }

  onCalendarTouchEnd() {
    clearTimeout(this.longPressTimer);
  }

  // =====================================================
  // 📱 MOBILE LONG PRESS
  // =====================================================
  private longPressTimer: any;
  private touchStartDate: Date | null = null;

  openCreateFromFab() {
    this.aplicarEstadoFormulario();
    this.handleDateInteraction(new Date());
  }
  handleDateInteraction(date: Date) {

    // 🔥 se permite crear ordenes hasta 24h atras (ingreso retroactivo).
    if (this.esFechaDemasiadoAntigua(date)) {
      Swal.fire({
        toast: true,
        position: 'top-end',
        icon: 'warning',
        title: 'No puedes usar fechas pasadas',
        timer: 2000,
        showConfirmButton: false
      });
      return;
    }

    this.abrirModalCreacion(date);
  }

  // 🔒 true si la fecha es de mas de 24 horas atras (limite del ingreso
  // retroactivo, ver OrdenMantenimientoServiceImpl.construirOrdenRetroactiva).
  // Una fecha pasada pero dentro de esas 24h SI se permite: el usuario
  // podra marcar "Ingreso retroactivo" dentro del modal para declararla.
  private esFechaDemasiadoAntigua(fecha: Date): boolean {
    const veinticuatroHorasMs = 24 * 60 * 60 * 1000;
    return fecha.getTime() < Date.now() - veinticuatroHorasMs;
  }

  formatearMilesBlock(valor: any): string {

    if (valor === null || valor === undefined || valor === '') {
      return '';
    }

    return Number(valor).toLocaleString('es-CL');
  }

  limpiarFormulario(): void {
    this.ordenMantencionForm.reset();
    this.repuestosFormArray.clear();
    this.activoControl.reset();
    this.proveedorControl.reset();
    this.tipoMantenimientoControl.reset();

    // 🔥 reset() solo limpia VALORES: los validadores de fechaHora /
    // duracionMinutos vs fechaEjecucionReal / fechaFinEjecucionReal
    // hay que devolverlos al estado "normal" (no retroactivo) a mano.
    this.onToggleIngresoRetroactivo();
  }

  // 🔥 Ingreso retroactivo: al marcarlo, "Fecha"/"Duración (min)" dejan
  // de ser obligatorias (la orden ya no se programa, ya se hizo) y en
  // su lugar se exigen el inicio y termino REALES del trabajo.
  onToggleIngresoRetroactivo(): void {

    const retroactivo = !!this.ordenMantencionForm.get('ingresoRetroactivo')?.value;

    const fechaHora = this.ordenMantencionForm.get('fechaHora');
    const duracionMinutos = this.ordenMantencionForm.get('duracionMinutos');
    const fechaEjecucionReal = this.ordenMantencionForm.get('fechaEjecucionReal');
    const fechaFinEjecucionReal = this.ordenMantencionForm.get('fechaFinEjecucionReal');

    if (retroactivo) {
      fechaHora?.clearValidators();
      duracionMinutos?.clearValidators();
      fechaEjecucionReal?.setValidators([Validators.required]);
      fechaFinEjecucionReal?.setValidators([Validators.required]);
    } else {
      fechaHora?.setValidators([Validators.required]);
      duracionMinutos?.setValidators([Validators.required, Validators.pattern('^[0-9]+$')]);
      fechaEjecucionReal?.clearValidators();
      fechaFinEjecucionReal?.clearValidators();
    }

    fechaHora?.updateValueAndValidity();
    duracionMinutos?.updateValueAndValidity();
    fechaEjecucionReal?.updateValueAndValidity();
    fechaFinEjecucionReal?.updateValueAndValidity();
  }

  formatFechaLocal(date: Date): string {
    const pad = (n: number) => n.toString().padStart(2, '0');

    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
  }

  ngOnDestroy(): void {
    this.usuarioSub?.unsubscribe();
  }
}
