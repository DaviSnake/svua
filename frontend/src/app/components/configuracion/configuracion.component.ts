import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import Swal from 'sweetalert2';
import { ConfiguracionService } from '../../services/configuracion.service';
import { ConfiguracionEntry } from './models/configuracion.model';

// 🔒 Claves consideradas sensibles (contraseñas, secretos, API keys):
// se muestran enmascaradas por defecto, con un botón para revelarlas
// (mismo patrón del campo de contraseña del login).
const PATRON_SENSIBLE = /PASSWORD|SECRET|KEY/i;

@Component({
  selector: 'app-configuracion',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './configuracion.component.html',
  styleUrl: './configuracion.component.css'
})
export class ConfiguracionComponent implements OnInit {

  private configuracionService = inject(ConfiguracionService);

  entradas: ConfiguracionEntry[] = [];
  private valoresOriginales: Record<string, string> = {};
  visibles: Record<string, boolean> = {};

  cargando = false;
  guardando = false;

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {

    this.cargando = true;

    this.configuracionService.obtenerConfiguracion().subscribe({
      next: (data) => {

        this.entradas = data;
        this.valoresOriginales = Object.fromEntries(
          data.map(e => [e.clave, e.valor])
        );

        this.cargando = false;
      },
      error: () => {
        this.cargando = false;

        Swal.fire({
          icon: 'error',
          title: 'Error',
          text: 'No se pudo cargar la configuración.'
        });
      }
    });
  }

  esSensible(clave: string): boolean {
    return PATRON_SENSIBLE.test(clave);
  }

  toggleVisible(clave: string): void {
    this.visibles[clave] = !this.visibles[clave];
  }

  // 🔥 trackBy para la tabla de variables de configuración: la clave
  // (nombre de la variable) es un identificador estable, a diferencia
  // del índice del array.
  trackByClave(index: number, entrada: ConfiguracionEntry): string {
    return entrada?.clave ?? String(index);
  }

  hayCambios(): boolean {
    return this.entradas.some(
      e => e.valor !== this.valoresOriginales[e.clave]
    );
  }

  guardar(): void {

    const cambios: Record<string, string> = {};

    this.entradas.forEach(e => {
      if (e.valor !== this.valoresOriginales[e.clave]) {
        cambios[e.clave] = e.valor;
      }
    });

    if (Object.keys(cambios).length === 0) {
      return;
    }

    this.guardando = true;

    this.configuracionService.actualizarConfiguracion(cambios).subscribe({
      next: () => {

        this.guardando = false;

        this.valoresOriginales = Object.fromEntries(
          this.entradas.map(e => [e.clave, e.valor])
        );

        Swal.fire({
          icon: 'success',
          title: 'Configuración guardada',
          text: 'Los cambios se guardaron en el .env. Para que tomen efecto es necesario reiniciar el backend.',
          confirmButtonText: 'Entendido'
        });
      },
      error: (err) => {

        this.guardando = false;

        Swal.fire({
          icon: 'error',
          title: 'Error',
          text: err.error?.error || 'No se pudo guardar la configuración.'
        });
      }
    });
  }

}
