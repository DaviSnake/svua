import { Component, HostListener, inject, OnInit, ViewChild } from '@angular/core';
import { CalendarOptions } from '@fullcalendar/core';

import { FullCalendarComponent, FullCalendarModule } from '@fullcalendar/angular';

import dayGridPlugin from '@fullcalendar/daygrid';
import interactionPlugin from '@fullcalendar/interaction';
import timeGridPlugin from '@fullcalendar/timegrid';
import esLocale from '@fullcalendar/core/locales/es';
import listPlugin from '@fullcalendar/list';

import { OrdenMantencionService } from '../../services/orden-mantencion.service';
import { FormArray, FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ActivoService } from '../../services/activo.service';
import { Activo } from '../../model/activo';
import { OrdenResponse } from '../../model/ordenResponse';

import { MatAutocompleteModule, MatAutocompleteTrigger } from '@angular/material/autocomplete';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { AuthService } from '../../services/auth.service';
import Swal from 'sweetalert2';
import { RepuestoService } from '../../services/repuesto.service';
import { ProveedorService } from '../../services/proveedor.service';
import { FormUtils } from '../../shared/form-utils';

@Component({
  selector: 'app-calendario',
  standalone: true,
  imports: [FullCalendarModule, CommonModule, ReactiveFormsModule, MatAutocompleteModule, MatInputModule, MatFormFieldModule],
  templateUrl: './calendario.component.html',
  styleUrls: ['./calendario.component.css']
})
export class CalendarioComponent implements OnInit {

  private ordenMantencionService = inject(OrdenMantencionService);
  private activoService = inject(ActivoService);
  private repuestoService = inject(RepuestoService);
  private proveedorService = inject(ProveedorService);
  private authService = inject(AuthService);
  private fb = inject(FormBuilder);

  usuario: any;

  activos: Activo[] = [];

  repuestos: any[] = [];

  proveedores: any[] = [];

  activoControl = new FormControl();
  activosFiltrados: Activo[] = [];

  estadoOrden: string = 'PENDIENTE';

  riesgo!: number;
  nivel!: string;
  costoTotal!: string;

  orden!: OrdenResponse;

  @ViewChild('calendar') calendarComponent!: FullCalendarComponent;
  @ViewChild(MatAutocompleteTrigger) trigger!: MatAutocompleteTrigger;

  ngAfterViewInit() {
    setTimeout(() => {
      this.calendarComponent?.getApi().render();
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

    api.changeView(this.getCalendarView());

    setTimeout(() => api.updateSize(), 100);
  }

  // =====================================================
  // 🎨 EVENT RENDER (TOOLTIP + STYLE)
  // =====================================================

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
    <strong>${estado}</strong><br>
    Tipo: ${tipo}<br>
    Duración: ${duracion} min
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
    editable: true,
    selectable: true,   // 🔥 CLAVE
    navLinks: true,
    locale: esLocale,
    height: 'auto',   // 🔥 IMPORTANTE
    expandRows: true, // 🔥 IMPORTANTE
    contentHeight: 'auto',
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
        <strong>${estado}</strong><br>
        Tipo: ${tipo}<br>
        Duración: ${duracion} min
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

  page = 0;
  size = 100000;

  ngOnInit(): void {
    // usuario
    this.authService.user$.subscribe(user => {
      this.usuario = user;
    });

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
      valorHora: ['',[Validators.required, Validators.pattern('^[0-9]+$')]],
      horasEstimadas: ['',[Validators.required, Validators.pattern(/^\d+([.,]\d+)?$/)]],
      horas: [''],
      costoManoObraEstimada: ['',[Validators.required, Validators.pattern('^[0-9]+$')]],
      costoManoObra: [''],
      tipoMantenimiento: [null, Validators.required],
      repuestos: this.fb.array([])
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

    this.ordenMantencionForm.get('duracionMinutos')?.valueChanges
      .subscribe(valor => {

        if (!['COMPLETADA', 'CANCELADA', 'EN_EJECUCION']
              .includes(this.estadoOrden)) {

          this.transformarAHora(); // 👈 lo que quieras ejecutar          
        }
      });

    this.ordenMantencionForm.get('valorHora')?.valueChanges
      .subscribe(valor => {

        if (!['COMPLETADA', 'CANCELADA', 'EN_EJECUCION']
              .includes(this.estadoOrden)) {

          this.calcularCostoManoObra();
        }
      });

    this.cargarEventos();
    this.cargarActivos();
    this.cargarRepustos();
    this.cargarProveedores();

  }

  

  onSelectRange(info: any) {

  const fecha = info.start;

  const ahora = new Date();

  if (fecha < ahora) {
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
    const valorHora = (this.ordenMantencionForm.get('valorHora')?.value || 0);
    const horaEstimada = (this.ordenMantencionForm.get('horasEstimadas')?.value || 0);
    const costoManoObraEstimada = valorHora * horaEstimada;

    this.ordenMantencionForm.patchValue({
      costoManoObraEstimada: costoManoObraEstimada
    });
  }

  cargarProveedores() {
      this.proveedorService.getAll(this.page, this.size).subscribe({
        next: (data) => {
          this.proveedores = data.content;
  
          //console.log("DATA:", this.proveedores)
        },
        error: (err) => {
          Swal.fire({
            icon: 'error',
            title: 'Error',
            text: err.error?.message || 'Error desconocido'
          });
  
          console.log("ERROR:", err);
        }
      });
    }

  // 🔥 CARGAR EVENTOS (solo actualiza events)
  cargarEventos() {
    this.ordenMantencionService.listar().subscribe((ordenesMantencion) => {

      const eventos = ordenesMantencion || [];

      this.calendarOptions.events = eventos.map(ordenMantencion => ({
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
          repuestos: ordenMantencion.repuestos
        }
      }));
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

    const ahora = new Date();

    // 🔥 comparar fechas
    if (fecha < ahora) {
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

    this.mostrarModal = true;
  }

  // 🟡 MOVER EVENTO
  onEventDrop(info: any) {
    if (!info.event.start) return;

    const id = info.event.id;
    const nuevaFecha = info.event.start;

    const ahora = new Date();

    this.estadoOrden = info.event.extendedProps?.estado;

    // 🔥 comparar fechas
    if (nuevaFecha < ahora) {
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

    if (this.estadoOrden === 'EN_EJECUCION' || this.estadoOrden === 'COMPLETADA') {
      info.revert();
      Swal.fire({
        toast: true,
        position: 'top-end',
        icon: 'warning',
        title: 'No puedes Reprogramar',
        showConfirmButton: false,
        timer: 2500
      });
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
      horasEstimadas: info.event.extendedProps?.horasEstimadas || '',
      horas: info.event.extendedProps?.horasReal || '',
      costoManoObraEstimada: info.event.extendedProps?.costoManoObraEstimada || '',
      costoManoObra: info.event.extendedProps?.costoManoObra || '',
      fechaHora: fechaLocal
    });

    // 🔥 AQUÍ LA MAGIA
    this.setActivoSeleccionado(activoId);

    this.aplicarEstadoFormulario();

    this.mostrarModal = true;
  }

  // 💾 GUARDAR (CREAR / EDITAR)
  guardar() {
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
            fechaHora, valorHora, horasEstimadas, costoManoObraEstimada } = this.ordenMantencionForm.getRawValue();

    const data = {
      titulo,
      fechaProgramada: fechaHora,
      duracionMinutos,
      tipoMantenimiento,
      estado: "PROGRAMADA",
      observaciones,
      activoId,
      proveedorId,
      usuarioId: this.usuario.sub,
      planMantenimientoId: "1",
      valorHora, 
      horasEstimadas, 
      costoManoObraEstimada: costoManoObraEstimada,
      // 🔥 NUEVO
      repuestos: this.repuestosFormArray.getRawValue()
    };

    if (this.modoEdicion) {
      // 🔵 EDITAR
      this.ordenMantencionService.actualizar(this.eventoSeleccionadoId, data)
        .subscribe(() => {
          this.cargarEventos();
          this.cerrar();
        });
    } else {
      const activoSeleccionado = this.activoControl.value;

      this.ordenMantencionForm.patchValue({
        activoId: activoSeleccionado?.id || null
      });

      // 🟢 CREAR
      this.ordenMantencionService.crear(data)
        .subscribe(() => {
          this.repuestosFormArray.clear();
          this.cargarEventos();
          this.cerrar();
        });
    }
  }

  private abrirModalCreacion(fecha: Date) {

    this.limpiarFormulario();
    this.activoControl.reset();

    this.ordenMantencionForm.patchValue({
      fechaHora: this.formatFechaLocal(fecha)
    });

    this.modoEdicion = false;
    this.mostrarModal = true;

    setTimeout(() => {
      this.calendarComponent?.getApi()?.updateSize();
    }, 100);
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

    setTimeout(() => {
      this.calendarComponent?.getApi().updateSize();
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

      case 'COMPLETADA':
        return '#22c55e'; // verde

      case 'CANCELADA':
        return '#ef4444'; // rojo

      default:
        return '#6b7280'; // gris
    }
  }

  get isReadOnly(): boolean {
    return ['PROGRAMADA', 'COMPLETADA', 'CANCELADA', 'EN_EJECUCION'].includes(this.estadoOrden);
  }

  ngOnChanges() {
    this.aplicarEstadoFormulario();
  }



  getColor(
    estado?: string,
    tipoMantenimiento?: string
  ): string {

    switch (`${estado}-${tipoMantenimiento}`) {

      case 'PENDIENTE-PREVENTIVO':
        return '#eab308';

      case 'PENDIENTE-CORRECTIVO':
        return '#ef4444';

      case 'PROGRAMADA-PREVENTIVO':
        return '#3b82f6';

      case 'EN_EJECUCION-CORRECTIVO':
        return '#f97316';

      case 'COMPLETADA-PREVENTIVO':
      case 'COMPLETADA-CORRECTIVO':
        return '#22c55e';

      case 'CANCELADA-PREVENTIVO':
      case 'CANCELADA-CORRECTIVO':
        return '#6b7280';

      default:
        return '#9ca3af';
    }
  }

  toggleMantencion() {
    const id = this.eventoSeleccionadoId;
    if (this.estadoOrden === 'EN_EJECUCION') {
      this.confirmarDetencionConArchivo(id);
    } else {
      this.iniciarMantencion(id);
    }

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
      error: () => {
        Swal.fire({
          toast: true,
          position: 'top-end',
          icon: 'error',
          title: 'No se pudo detener la mantención',
          showConfirmButton: false,
          timer: 2500
        });
      }
    });
  }

  detenerMantencionConArchivo(id: number, archivo: File) {

  const formData = new FormData();
  formData.append('archivo', archivo);

  this.ordenMantencionService.detenerConArchivo(id, formData)
    .subscribe({
      next: () => {
        Swal.fire({
          icon: 'success',
          title: 'Mantención detenida',
          timer: 1500,
          showConfirmButton: false
        });

        console.log('Detenido OK');
        this.cargarEventos();
        this.cerrar();
      },
      error: (err: { error: { message: any; }; }) => {
        Swal.fire({
          icon: 'error',
          title: err.error?.message || 'Error al detener'
        });
      }
    });
}

  confirmarDetencionConArchivo(id: number) {
    Swal.fire({
      title: 'Ingrese dcto de chequeo de mantención',
      html: `
        <input type="file" id="fileInput" class="swal2-file" />
      `,
      confirmButtonText: 'Guardar',
      showCancelButton: true,
      cancelButtonText: 'Cancelar',
      preConfirm: () => {
        const input = document.getElementById('fileInput') as HTMLInputElement;

        if (!input || !input.files || input.files.length === 0) {
          Swal.showValidationMessage('Debes subir un archivo');
          return false;
        }

        return input.files[0]; // 🔥 devolvemos el archivo
      }
    }).then((result) => {

      if (!result.isConfirmed) return;

      const archivo: File = result.value;

      this.detenerMantencionConArchivo(id, archivo);
    });
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

  puedeEditar(): boolean {
    if (!this.modoEdicion) return true; // 🔥 nueva orden

    return this.estadoOrden === 'PENDIENTE' || 
          this.estadoOrden === 'PROGRAMADA';
  }

  aplicarEstadoFormulario() {
    if (!this.isReadOnly) {
      this.ordenMantencionForm.enable();
      this.activoControl.enable();
    } else {
      this.ordenMantencionForm.disable();
      this.activoControl.disable();
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

    this.repuestosFormArray?.removeAt(index);
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
      const repuesto = this.fb.group({
        repuestoId: [repuestoId],
        cantidad: [cantidad]
      });
      this.repuestosFormArray?.push(repuesto);
    }

    // 🔥 limpiar form
    this.repuestoForm.reset({
      repuestoId: null,
      cantidad: 1
    });

    this.cerrarModalRepuesto();
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
    const now = new Date();

    if (date < now) {
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
  }  

  formatFechaLocal(date: Date): string {
    const pad = (n: number) => n.toString().padStart(2, '0');

    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
  }
}

