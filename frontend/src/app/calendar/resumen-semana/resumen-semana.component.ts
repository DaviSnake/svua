import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';

// 📅 RESUMEN POR DÍA: franja de semana (barritas o insignia con
// numero segun cuantas ordenes tiene cada dia) + tarjeta de detalle
// del dia elegido. Componente "tonto": recibe la lista plana de
// ordenes (mismo shape que calendarOptions.events en CalendarioComponent)
// y solo emite hacia afuera cuando el usuario quiere abrir una orden o
// ver el dia completo en FullCalendar -- todo lo demas (que semana se
// esta mirando, que dia esta seleccionado) es estado propio.
@Component({
  selector: 'app-resumen-semana',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './resumen-semana.component.html',
  styleUrl: './resumen-semana.component.css'
})
export class ResumenSemanaComponent implements OnChanges {

  private static readonly UMBRAL_INSIGNIA = 3;

  @Input() ordenes: any[] = [];
  // 🔥 semana en la que aterrizar al activarse (ver resumenFechaFoco en
  // CalendarioComponent) -- null/undefined significa "semana actual".
  @Input() fechaInicial: Date | null = null;

  @Output() ordenSeleccionada = new EventEmitter<any>();
  @Output() verDiaCompleto = new EventEmitter<Date>();
  @Output() crearOrden = new EventEmitter<Date>();

  semana: Date[] = [];
  diaSeleccionado: Date | null = null;
  ordenesDelDia: any[] = [];

  get tituloSemana(): string {
    if (!this.semana.length) return '';

    const inicio = this.semana[0];
    const fin = this.semana[6];
    const capitalizar = (s: string) => s.charAt(0).toUpperCase() + s.slice(1);
    const mesCorto = (d: Date) => d.toLocaleDateString('es-CL', { month: 'short' }).replace('.', '');

    if (inicio.getMonth() === fin.getMonth() && inicio.getFullYear() === fin.getFullYear()) {
      return capitalizar(`${inicio.toLocaleDateString('es-CL', { month: 'long' })} ${fin.getFullYear()}`);
    }

    const sufijoAnio = inicio.getFullYear() === fin.getFullYear() ? '' : ` ${inicio.getFullYear()}`;
    return `${inicio.getDate()} ${mesCorto(inicio)}${sufijoAnio} – ${fin.getDate()} ${mesCorto(fin)} ${fin.getFullYear()}`;
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (!this.semana.length) {
      this.semana = this.calcularSemana(this.fechaInicial ?? new Date());
    }

    if (changes['ordenes']) {
      // 🔥 si ya habia un dia elegido, se recalcula su detalle con los
      // datos nuevos en vez de perder la seleccion al recargar. Al
      // entrar por primera vez se para en HOY (no en el dia con mas
      // ordenes) -- la semana inicial siempre contiene hoy, ver
      // calcularSemana(new Date()) arriba.
      this.seleccionarDia(this.diaSeleccionado ?? this.diaDeHoyEnSemana(this.semana) ?? this.semana[0]);
    }
  }

  private calcularSemana(fechaBase: Date): Date[] {
    const dia = fechaBase.getDay(); // 0=domingo..6=sabado
    const offsetLunes = dia === 0 ? -6 : 1 - dia;

    const lunes = new Date(fechaBase);
    lunes.setHours(0, 0, 0, 0);
    lunes.setDate(lunes.getDate() + offsetLunes);

    return Array.from({ length: 7 }, (_, i) => {
      const d = new Date(lunes);
      d.setDate(lunes.getDate() + i);
      return d;
    });
  }

  private mismoDia(a: Date, b: Date): boolean {
    return a.getFullYear() === b.getFullYear()
      && a.getMonth() === b.getMonth()
      && a.getDate() === b.getDate();
  }

  ordenesDeDia(fecha: Date): any[] {
    return (this.ordenes || [])
      .filter(o => o.start && this.mismoDia(new Date(o.start), fecha))
      .sort((a, b) => new Date(a.start).getTime() - new Date(b.start).getTime());
  }

  private diaDeHoyEnSemana(semana: Date[]): Date | null {
    const hoy = new Date();
    return semana.find(dia => this.mismoDia(dia, hoy)) ?? null;
  }

  usaInsignia(fecha: Date): boolean {
    return this.ordenesDeDia(fecha).length > ResumenSemanaComponent.UMBRAL_INSIGNIA;
  }

  barritas(fecha: Date): number[] {
    return Array(Math.min(this.ordenesDeDia(fecha).length, ResumenSemanaComponent.UMBRAL_INSIGNIA)).fill(0);
  }

  esHoy(fecha: Date): boolean {
    return this.mismoDia(fecha, new Date());
  }

  esSeleccionado(fecha: Date): boolean {
    return !!this.diaSeleccionado && this.mismoDia(fecha, this.diaSeleccionado);
  }

  seleccionarDia(fecha: Date): void {
    this.diaSeleccionado = fecha;
    this.ordenesDelDia = this.ordenesDeDia(fecha);
  }

  semanaAnterior(): void {
    const anterior = new Date(this.semana[0]);
    anterior.setDate(anterior.getDate() - 7);
    this.semana = this.calcularSemana(anterior);
    this.seleccionarDia(this.semana[0]);
  }

  semanaSiguiente(): void {
    const siguiente = new Date(this.semana[0]);
    siguiente.setDate(siguiente.getDate() + 7);
    this.semana = this.calcularSemana(siguiente);
    this.seleccionarDia(this.semana[0]);
  }

  hoy(): void {
    this.semana = this.calcularSemana(new Date());
    this.seleccionarDia(this.diaDeHoyEnSemana(this.semana) ?? this.semana[0]);
  }

  abrirOrden(orden: any): void {
    // 🔥 onEventClick (reusado del padre) espera info.event.start como
    // Date -- FullCalendar lo entrega asi, pero aca "orden.start" sigue
    // siendo el string ISO crudo del mapeo de ordenesResumen.
    this.ordenSeleccionada.emit({ ...orden, start: new Date(orden.start) });
  }

  pedirDiaCompleto(): void {
    this.verDiaCompleto.emit(this.diaSeleccionado ?? this.semana[0]);
  }

  pedirCrearOrden(): void {
    this.crearOrden.emit(this.diaSeleccionado ?? this.semana[0]);
  }
}
