import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import Swal from 'sweetalert2';
import { ManualGridColumn, ManualGridImportFn, ManualGridRowResult } from '../../model/manualGrid';

/**
 * Grilla genérica de ingreso en tiempo real (tipo planilla) reutilizada por
 * todas las cargas masivas (Activos, Proveedores, Órdenes, Repuestos,
 * Ubicaciones y Tipos de Activo). Se configura por medio de `columns`
 * (columnas + tipo de dato + opciones de autocompletado) y `importFn`
 * (llamada al backend que guarda el lote). Todo el comportamiento de
 * copiar/pegar celdas y el diseño responsive (tablet/celular) vive acá,
 * en un solo lugar.
 */
@Component({
  selector: 'app-manual-grid',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './manual-grid.component.html',
  styleUrl: './manual-grid.component.css'
})
export class ManualGridComponent implements OnInit {

  // Contador estático para generar ids únicos de <datalist> por instancia,
  // ya que puede haber varias grillas en la misma pantalla usando la misma
  // clave de columna (ej. "proveedorRut" en Activos y en Órdenes).
  private static nextId = 0;
  instanceId = 'mg' + ManualGridComponent.nextId++;

  @Input({ required: true }) columns: ManualGridColumn[] = [];
  @Input({ required: true }) importFn!: ManualGridImportFn;
  /** Key (de columns) usada para saber si una fila tiene datos o está vacía */
  @Input({ required: true }) campoRequerido = '';
  @Input() hint = 'Escribe los datos de cada fila o copia celdas desde un Excel y pégalas directamente sobre la tabla. Los campos marcados con * son obligatorios.';
  /** Encabezado de la columna de referencia en la tabla de resultados */
  @Input() etiquetaReferencia = 'Referencia';
  @Input() textoBoton = 'Guardar';

  filas: any[] = [];
  enviando = false;
  resultados: ManualGridRowResult[] = [];
  resumen = '';

  ngOnInit() {
    this.agregarFila();
  }

  private nuevaFila(): any {
    const fila: any = {};

    for (const col of this.columns) {
      fila[col.key] = col.type === 'number' ? null : '';
    }

    this.recalcularFila(fila);

    return fila;
  }

  agregarFila() {
    this.filas.push(this.nuevaFila());
  }

  /**
   * Recalcula, en orden, todas las columnas que declaran `calcular`. Se usa
   * tanto al tipear/pegar como al crear una fila nueva. El orden en que
   * están declaradas las columnas importa: si una columna calculada depende
   * de otra columna calculada (ej. costo = valorHora × horasEstimadas, y
   * horasEstimadas a su vez sale de duracionMinutos), esta última debe
   * declararse antes en `columns` para que ya tenga el valor actualizado.
   */
  private recalcularFila(fila: any) {
    for (const col of this.columns) {
      if (col.calcular) {
        fila[col.key] = col.calcular(fila);
      }
    }
  }

  /**
   * Maneja el cambio de un campo editable: actualiza su valor y dispara el
   * recálculo de las columnas calculadas de esa misma fila.
   */
  onCampoChange(fila: any, col: ManualGridColumn, valor: any) {
    fila[col.key] = valor;
    this.recalcularFila(fila);
  }

  // Evita que Angular destruya y reconstruya las columnas/celdas cuando no
  // han cambiado realmente (defensa adicional junto con el fix de arriba).
  trackByColKey(_index: number, col: ManualGridColumn) {
    return col.key;
  }

  // 🔥 trackBy para la tabla de resultados del envío (se reemplaza por
  // completo en cada "Guardar", así que sin esto Angular recrea todas
  // las filas del DOM en cada intento).
  trackByResultadoIndex(index: number, _r: ManualGridRowResult) {
    return index;
  }

  eliminarFila(index: number) {
    this.filas.splice(index, 1);

    if (this.filas.length === 0) {
      this.agregarFila();
    }
  }

  // Permite pegar un rango de celdas copiado desde un Excel real directamente sobre la grilla
  onPaste(event: ClipboardEvent, filaInicio: number, colInicio: number) {

    const texto = event.clipboardData?.getData('text');

    if (!texto || (!texto.includes('\t') && !texto.includes('\n'))) {
      // pegado de una sola celda: dejar que el input maneje el paste normal
      return;
    }

    event.preventDefault();

    const lineas = texto.replace(/\r/g, '').split('\n').filter(l => l.length > 0);

    lineas.forEach((lineaTexto, i) => {
      const celdas = lineaTexto.split('\t');
      const filaIndex = filaInicio + i;

      while (this.filas.length <= filaIndex) {
        this.agregarFila();
      }

      const fila = this.filas[filaIndex];

      celdas.forEach((valor, j) => {
        const col = this.columns[colInicio + j];

        // Las columnas calculadas (de solo lectura) no se pisan al pegar:
        // se recalculan solas apenas terminamos de aplicar el pegado.
        if (!col || col.readonly) return;

        if (col.type === 'number') {
          const num = Number(valor.replace(',', '.').trim());
          fila[col.key] = isNaN(num) ? null : num;
        } else {
          fila[col.key] = valor.trim();
        }
      });

      this.recalcularFila(fila);
    });
  }

  enviar() {

    const campo = this.campoRequerido;

    const filasParaEnviar = this.filas.filter(f => (f[campo] ?? '').toString().trim());
    const filasVacias = this.filas.filter(f => !(f[campo] ?? '').toString().trim());

    if (filasParaEnviar.length === 0) {
      Swal.fire({
        icon: 'warning',
        title: 'Sin datos',
        text: 'Agrega al menos una fila con datos.'
      });
      return;
    }

    this.enviando = true;
    this.resultados = [];
    this.resumen = '';

    this.importFn(filasParaEnviar).subscribe({
      next: (res) => {
        this.enviando = false;
        this.resultados = res.resultados;
        this.resumen = `${res.exitosos} de ${res.total} registro(s) guardado(s) correctamente`;

        // Dejamos en la grilla solo las filas que fallaron, para poder corregirlas y reintentar
        const filasConError = filasParaEnviar.filter((_, i) => !res.resultados[i]?.exito);

        this.filas = filasConError.length > 0
          ? [...filasConError, ...filasVacias]
          : [this.nuevaFila()];

        if (res.fallidos === 0) {
          Swal.fire({
            icon: 'success',
            title: '¡Guardado!',
            text: `${res.exitosos} registro(s) guardado(s) correctamente`,
            timer: 2000,
            showConfirmButton: false
          });
        }
      },
      error: () => {
        this.enviando = false;
        Swal.fire({
          icon: 'error',
          title: 'Error',
          text: 'No se pudo procesar la carga manual'
        });
      }
    });
  }

}
