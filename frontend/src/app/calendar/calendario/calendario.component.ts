import { Component, inject, OnInit } from '@angular/core';
import { CalendarOptions } from '@fullcalendar/core';

import { FullCalendarModule } from '@fullcalendar/angular';

import dayGridPlugin from '@fullcalendar/daygrid';
import interactionPlugin from '@fullcalendar/interaction';
import timeGridPlugin from '@fullcalendar/timegrid';
import esLocale from '@fullcalendar/core/locales/es';

import { OrdenMantencionService } from '../../services/orden-mantencion.service';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { FormUtils } from '../../shared/form-utils';
import { ActivoService } from '../../services/activo.service';
import { Activo } from '../../model/activo';
import { OrdenResponse } from '../../model/ordenResponse';

@Component({
  selector: 'app-calendario',
  standalone: true,
  imports: [FullCalendarModule, CommonModule, ReactiveFormsModule],
  templateUrl: './calendario.component.html',
  styleUrl: './calendario.component.css'
})
export class CalendarioComponent implements OnInit {

  private ordenMantencionService = inject(OrdenMantencionService);
  private activoService = inject(ActivoService);
  private fb = inject(FormBuilder);

  activos: Activo[] = [];

  estadoOrden: string = 'PENDIENTE';

  orden!: OrdenResponse;

  // 🔹 CALENDARIO (inicializado desde el inicio 🔥)
  calendarOptions: CalendarOptions = {
    plugins: [dayGridPlugin, timeGridPlugin, interactionPlugin],
    initialView: 'timeGridWeek',
    initialDate: new Date(), // ✅ semana actual
    editable: true,
    locale: esLocale,
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
    this.ordenMantencionForm = this.fb.group({
      titulo: ['', Validators.required],
      observaciones: [''],
      lugar: [''],
      estado: [''],
      hora: ['', Validators.required],
      activoId: [null, Validators.required],
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

        // 🎨 color dinámico
        backgroundColor: this.getColorPorEstado(ordenMantencion.estado),
        borderColor: this.getColorPorEstado(ordenMantencion.estado),
        textColor: '#fff',

        extendedProps: {
          estado: ordenMantencion.estado,
          tipoMantenimiento: ordenMantencion.tipoMantenimiento,
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

    const fechaISO = fecha.toISOString().split('T')[0];
    const hora = fecha.toTimeString().slice(0, 5);

    this.fechaSeleccionada = fechaISO;
    this.modoEdicion = false;

    this.ordenMantencionForm.reset();
    this.ordenMantencionForm.patchValue({ hora });

    this.mostrarModal = true;
  }

  // 🟡 MOVER EVENTO
  onEventDrop(info: any) {
    if (!info.event.start) return;

    const id = info.event.id;
    const fecha = info.event.start;

    this.ordenMantencionService.reprogramar(id, fecha).subscribe({
      next: () => {
        console.log('Reprogramado OK');
      },
      error: () => {
        info.revert(); // 🔥 vuelve atrás si falla
        alert('No se pudo reprogramar');
      }
    });

  }

  // 🔵 CLICK EN EVENTO (EDITAR)
  onEventClick(info: any) {

    const fecha = info.event.start;
    if (!fecha) return;

    const fechaISO = fecha.toISOString().split('T')[0];
    const hora = fecha.toTimeString().slice(0, 5);

    this.estadoOrden = info.event.extendedProps?.estado;

    this.fechaSeleccionada = fechaISO;
    this.eventoSeleccionadoId = Number(info.event.id);
    this.modoEdicion = true;

    this.ordenMantencionForm.patchValue({
      titulo: info.event.title,
      observaciones: info.event.extendedProps?.observaciones || '',
      estado: this.estadoOrden,
      lugar: '',
      hora: hora
    });

    this.mostrarModal = true;
  }

  // 💾 GUARDAR (CREAR / EDITAR)
  guardar() {
    if (this.ordenMantencionForm.invalid) {
      this.ordenMantencionForm.markAllAsTouched();
      return;
    }

    const { titulo, observaciones, lugar, hora } = this.ordenMantencionForm.value;

    const data = {
      titulo,
      observaciones,
      lugar,
      fechaProgramada: `${this.fechaSeleccionada}T${hora}`
    };

    console.log(data);

    if (this.modoEdicion) {
      // 🔵 EDITAR
      this.ordenMantencionService.actualizar(this.eventoSeleccionadoId, data)
        .subscribe(() => {
          this.cargarEventos();
          this.cerrar();
        });
    } else {
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
    if (!confirm('¿Eliminar esta cita?')) return;

    this.ordenMantencionService.eliminar(this.eventoSeleccionadoId)
      .subscribe(() => {
        this.cargarEventos();
        this.cerrar();
      });
  }

  cerrar() {
    this.mostrarModal = false;
    this.modoEdicion = false;
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
      },
      error: () => {
        console.log("error");
      }
    });
  }

  toggleMantencion() {
    const id = this.eventoSeleccionadoId;
    if (this.estadoOrden === 'EN_EJECUCION') {
      this.detenerMantencion(id);
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
      error: () => {
        alert('No se pudo iniciar');
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
        alert('No se pudo detener');
      }
    });
  }

  get titulo(): string {
    if (this.estadoOrden === 'COMPLETADA') return 'Orden';
    if (this.modoEdicion) return 'Actualizar Orden';
    return 'Nueva Orden';
  }
}

