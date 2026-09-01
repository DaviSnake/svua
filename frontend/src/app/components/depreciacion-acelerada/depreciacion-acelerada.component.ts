import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import Swal from 'sweetalert2';
import { DepreciacionService } from '../../services/depreciacion.service';
import { ActivoService } from '../../services/activo.service';
import { Activo } from '../../model/activo';

// 🔥 Pantalla "Depreciación Acelerada" (SUPER_ADMIN / ADMIN_EMPRESA):
// ejecuta el backfill de depreciación acelerada tributaria (SII) sobre
// activos existentes que quedaron sin ese cronograma -- ver
// DepreciacionServiceImpl.generarDepreciacionAceleradaFaltante /
// generarDepreciacionAceleradaPorActivo en el backend.
@Component({
  selector: 'app-depreciacion-acelerada',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, MatAutocompleteModule],
  templateUrl: './depreciacion-acelerada.component.html',
  styleUrl: './depreciacion-acelerada.component.css'
})
export class DepreciacionAceleradaComponent implements OnInit {

  private depreciacionService = inject(DepreciacionService);
  private activoService = inject(ActivoService);

  generandoFaltantes = false;
  generandoActivo = false;

  activos: Activo[] = [];
  activosFiltrados: Activo[] = [];
  filtroActivoControl = new FormControl();
  activoSeleccionado: Activo | null = null;

  ngOnInit(): void {
    this.initFiltroActivo();
    this.cargarActivos();
  }

  cargarActivos(): void {
    this.activoService.getActivoCombo(0, 200).subscribe(data => {
      this.activos = data.content;
      this.activosFiltrados = data.content;
    });
  }

  displayActivo = (activo: any): string =>
    activo?.nombre ? `${activo.nombre} (${activo.codigoInterno ?? 's/c'})` : '';

  onFocusFiltroActivo(): void {
    this.activosFiltrados = this.activos;
  }

  initFiltroActivo(): void {
    this.filtroActivoControl.valueChanges.subscribe(value => {
      const esObjeto = value && typeof value === 'object';
      const search = (esObjeto ? value.nombre : value || '').toLowerCase().trim();

      this.activosFiltrados = !search
        ? this.activos
        : this.activos.filter(a =>
            a.nombre.toLowerCase().includes(search) ||
            (a.codigoInterno ?? '').toLowerCase().includes(search)
          );

      this.activoSeleccionado = esObjeto ? value : null;
    });
  }

  // Masivo: todos los activos de la empresa actual que aún no tienen
  // depreciación acelerada calculada.
  generarFaltantes(): void {
    Swal.fire({
      title: '¿Generar depreciación acelerada?',
      text: 'Se calculará el cronograma acelerado (SII) para todos los activos de tu empresa que aún no lo tengan. No afecta la depreciación normal ya calculada.',
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: 'Sí, generar',
      cancelButtonText: 'Cancelar'
    }).then(result => {
      if (!result.isConfirmed) {
        return;
      }

      this.generandoFaltantes = true;

      this.depreciacionService.generarAceleradaFaltante().subscribe({
        next: (cantidad) => {
          this.generandoFaltantes = false;

          Swal.fire({
            icon: 'success',
            title: cantidad > 0
              ? `Depreciación acelerada generada para ${cantidad} activo(s)`
              : 'No había activos pendientes',
            text: cantidad > 0
              ? 'Los activos que ya la tenían calculada no fueron modificados.'
              : 'Todos los activos de tu empresa ya tenían su depreciación acelerada calculada.'
          });
        },
        error: (err) => {
          this.generandoFaltantes = false;

          Swal.fire({
            icon: 'error',
            title: err.error?.error || 'No fue posible generar la depreciación acelerada'
          });
        }
      });
    });
  }

  // Puntual: un activo elegido del combo.
  generarParaActivo(): void {

    if (!this.activoSeleccionado?.id) {
      Swal.fire({
        icon: 'warning',
        title: 'Selecciona un activo de la lista'
      });
      return;
    }

    const activo = this.activoSeleccionado;

    this.generandoActivo = true;

    this.depreciacionService.generarAceleradaPorActivo(activo.id!).subscribe({
      next: () => {
        this.generandoActivo = false;

        Swal.fire({
          icon: 'success',
          title: `Depreciación acelerada generada para "${activo.nombre}"`
        });

        this.filtroActivoControl.setValue('');
        this.activoSeleccionado = null;
      },
      error: (err) => {
        this.generandoActivo = false;

        Swal.fire({
          icon: 'error',
          title: err.error?.error || 'No fue posible generar la depreciación acelerada'
        });
      }
    });
  }
}
