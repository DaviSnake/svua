import { Component, inject, OnInit, ViewChild } from '@angular/core';
import { CalendarOptions } from '@fullcalendar/core';

import { FullCalendarComponent, FullCalendarModule } from '@fullcalendar/angular';

import dayGridPlugin from '@fullcalendar/daygrid';
import interactionPlugin from '@fullcalendar/interaction';
import timeGridPlugin from '@fullcalendar/timegrid';
import esLocale from '@fullcalendar/core/locales/es';

import { OrdenMantencionService } from '../../services/orden-mantencion.service';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ActivoService } from '../../services/activo.service';
import { Activo } from '../../model/activo';
import { OrdenResponse } from '../../model/ordenResponse';

import { MatAutocompleteModule, MatAutocompleteTrigger } from '@angular/material/autocomplete';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { AuthService } from '../../services/auth.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-calendario',
  standalone: true,
  imports: [FullCalendarModule, CommonModule, ReactiveFormsModule, MatAutocompleteModule, MatInputModule, MatFormFieldModule],
  templateUrl: './calendario.component.html',
  styleUrl: './calendario.component.css'
})
export class CalendarioComponent implements OnInit {

  private ordenMantencionService = inject(OrdenMantencionService);
  private activoService = inject(ActivoService);
  authService = inject(AuthService);
  private fb = inject(FormBuilder);

  usuario: any;

  activos: Activo[] = [];

  activoControl = new FormControl();
  activosFiltrados: Activo[] = [];

  estadoOrden: string = 'PENDIENTE';

  riesgo!: number;
  nivel!: string;

  orden!: OrdenResponse;

  @ViewChild('calendar') calendarComponent!: FullCalendarComponent;
  @ViewChild(MatAutocompleteTrigger) trigger!: MatAutocompleteTrigger;

  ngAfterViewInit() {
    setTimeout(() => {
      this.calendarComponent?.getApi().render();
      this.trigger?.openPanel();
    }, 300);
  }

  // 🔹 CALENDARIO (inicializado desde el inicio 🔥)
  calendarOptions: CalendarOptions = {
    plugins: [dayGridPlugin, timeGridPlugin, interactionPlugin],
    initialView: 'timeGridWeek',
    initialDate: new Date(), // ✅ semana actual
    editable: true,
    locale: esLocale,
    height: 'auto',   // 🔥 IMPORTANTE
    expandRows: true, // 🔥 IMPORTANTE
    contentHeight: 'auto',
    events: [],

    // 🟢 CREAR
    dateClick: (info) => this.onDateClick(info),

    // 🟡 MOVER
    eventDrop: (info) => this.onEventDrop(info),

    // 🔵 EDITAR
    eventClick: (info) => this.onEventClick(info)
  };

  // 🔹 FORMULARIO
  ordenMantencionForm!: FormGroup;
  mostrarModal = false;
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
      duracionMinutos: ['',[Validators.required, Validators.pattern('^[0-9]+$')]],
      fechaHora: ['', Validators.required], // 🔥 nuevo
      activoId: [null, Validators.required],
      tipoMantenimiento: [null, Validators.required]
    });

    this.activoControl.valueChanges.subscribe(activo => {
      this.ordenMantencionForm.patchValue({
        activoId: activo?.id || null
      });
    });

    this.cargarEventos();
    this.cargarActivos();

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
          costo: ordenMantencion.costo,
          activoId: ordenMantencion.activoId,
          usuarioId: ordenMantencion.usuarioId
        }
      }));

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

    this.ordenMantencionForm.reset();
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

    if (activoId) {
      this.cargarRiesgo(activoId);
    }

     const fechaLocal = this.formatFechaLocal(fecha);

    this.estadoOrden = info.event.extendedProps?.estado;

    this.fechaSeleccionada = fechaLocal;
    this.eventoSeleccionadoId = Number(info.event.id);
    this.modoEdicion = true;

    this.ordenMantencionForm.patchValue({
      titulo: info.event.title,
      observaciones: info.event.extendedProps?.observaciones || '',
      estado: this.estadoOrden,
      duracionMinutos: info.event.extendedProps?.duracionMinutos || '',
      tipoMantenimiento: info.event.extendedProps?.tipoMantenimiento || '',
      fechaHora: fechaLocal
    });

    // 🔥 AQUÍ LA MAGIA
    this.setActivoSeleccionado(activoId);

    this.aplicarEstadoFormulario();

    this.mostrarModal = true;
  }

  // 💾 GUARDAR (CREAR / EDITAR)
  guardar() {
    if (this.ordenMantencionForm.invalid) {
      this.ordenMantencionForm.markAllAsTouched();
      return;
    }

    const { titulo, observaciones, activoId, tipoMantenimiento, duracionMinutos, fechaHora } = this.ordenMantencionForm.value;

    const data = {
      titulo,
      fechaProgramada: fechaHora,
      duracionMinutos,
      tipoMantenimiento,
      estado: "PROGRAMADA",
      observaciones,
      activoId,
      usuarioId: this.usuario.sub,
      planMantenimientoId: "1"
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
          this.cargarEventos();
          this.cerrar();
        });
    }
  }

  // ❌ ELIMINAR
  eliminar() {
    Swal.fire({
      title: '¿Eliminar orden?',
      text: 'Esta acción no se puede deshacer',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#ef4444',
      cancelButtonColor: '#64748b',
      confirmButtonText: 'Sí, eliminar',
      cancelButtonText: 'Cancelar'
    }).then((result) => {

      if (result.isConfirmed) {

        this.ordenMantencionService.eliminar(this.eventoSeleccionadoId)
          .subscribe(() => {

            Swal.fire({
              title: 'Eliminado',
              text: 'La orden fue eliminada correctamente',
              icon: 'success',
              timer: 1500,
              showConfirmButton: false
            });

            this.cargarEventos();
            this.cerrar();
          });

      }

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

  displayActivo(activo: any): string {
    return activo ? activo.nombre : '';
  }

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
      this.activoControl.setValue(activo);
    }
  }

  puedeEditar(): boolean {
    if (!this.modoEdicion) return true; // 🔥 nueva orden

    return this.estadoOrden === 'PENDIENTE' || 
          this.estadoOrden === 'PROGRAMADA';
  }

  aplicarEstadoFormulario() {
    if (this.puedeEditar()) {
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

  formatFechaLocal(date: Date): string {
    const pad = (n: number) => n.toString().padStart(2, '0');

    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
  }
}

