import { Component, ElementRef, OnDestroy, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BarcodeFormat, BrowserMultiFormatReader, DecodeHintType } from '@zxing/library';
import Swal from 'sweetalert2';
import { ActivoService } from '../../services/activo.service';
import { ActivoEscaneoResponse } from '../../model/activoEscaneo';
import { OrdenMantencion } from '../../model/ordenMantencion';

@Component({
  selector: 'app-escanear',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './escanear.component.html',
  styleUrl: './escanear.component.css'
})
export class EscanearComponent implements OnDestroy {

  @ViewChild('video') videoRef?: ElementRef<HTMLVideoElement>;

  codigoInput = '';
  camaraActiva = false;
  cargando = false;
  resultado: ActivoEscaneoResponse | null = null;

  // 🔥 Id de la orden cuyo detalle de repuestos esta expandido (null = ninguna).
  ordenExpandidaId: number | null = null;

  // 🔳 Un solo lector reconoce QR (codigoInterno en texto plano) y EAN13
  // (codigo numerico), sin saber de antemano cual de los dos se va a leer.
  private lector: any;

  constructor(private activoService: ActivoService) {
    const hints = new Map();
    hints.set(DecodeHintType.POSSIBLE_FORMATS, [BarcodeFormat.QR_CODE, BarcodeFormat.EAN_13]);
    this.lector = new BrowserMultiFormatReader(hints);
  }

  ngOnDestroy(): void {
    this.detenerCamara();
  }

  async activarCamara() {
    if (this.camaraActiva || !this.videoRef) return;

    try {
      await this.lector.decodeFromVideoDevice(
        undefined,
        this.videoRef.nativeElement,
        (resultado: any) => {
          // 🔥 el callback se llama muchas veces por segundo mientras no
          // detecta nada (eso es normal): solo actuamos cuando SI hay resultado.
          if (resultado) {
            const texto = resultado.getText();
            this.detenerCamara();
            this.codigoInput = texto;
            this.buscar(texto);
          }
        }
      );
      this.camaraActiva = true;
    } catch (error) {
      console.error('No se pudo activar la cámara', error);
      this.camaraActiva = false;
      Swal.fire({
        icon: 'error',
        title: 'Cámara no disponible',
        text: 'No se pudo acceder a la cámara del dispositivo. Revisa los permisos del navegador.'
      });
    }
  }

  detenerCamara() {
    if (this.camaraActiva) {
      this.lector.reset();
    }
    this.camaraActiva = false;
  }

  buscar(codigoParam?: string) {
    const codigo = (codigoParam ?? this.codigoInput).trim();
    if (!codigo) return;

    this.cargando = true;
    this.resultado = null;

    this.activoService.buscarPorCodigo(codigo).subscribe({
      next: (respuesta) => {
        this.resultado = respuesta;
        this.cargando = false;
      },
      error: (err) => {
        this.cargando = false;
        Swal.fire({
          icon: 'error',
          title: 'No encontrado',
          text: err.error?.error || 'No se encontró ningún activo con ese código'
        });
      }
    });
  }

  limpiar() {
    this.codigoInput = '';
    this.resultado = null;
  }

  // 🔥 Muestra/oculta, debajo de la fila de la orden, el detalle de
  // repuestos utilizados (mismo patron que el comprobante de Informe de
  // Mantenciones, en vez de un modal aparte).
  toggleRepuestos(id: number | undefined): void {
    if (id == null) {
      return;
    }

    this.ordenExpandidaId = this.ordenExpandidaId === id ? null : id;
  }

  estaExpandida(id: number | undefined): boolean {
    return id != null && this.ordenExpandidaId === id;
  }

  trackByOrdenId(index: number, orden: OrdenMantencion): any {
    return orden?.id ?? index;
  }

  trackByRepuestoId(index: number, repuesto: any): any {
    return repuesto?.id ?? repuesto?.repuestoId ?? index;
  }
}
