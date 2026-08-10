import { Component, inject, OnInit } from '@angular/core';
import { CargaMasivaService } from '../../services/carga-masiva.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { map } from 'rxjs/operators';

import { TipoActivoService } from '../../services/tipo-activo.service';
import { UbicacionService } from '../../services/ubicacion.service';
import { ProveedorService } from '../../services/proveedor.service';
import { ActivoService } from '../../services/activo.service';
import { TipoActivo } from '../../model/tipoActivo';
import { Ubicacion } from '../../model/ubicacion';
import { Proveedor } from '../../model/proveedor';
import { Activo } from '../../model/activo';
import { ActivoImportRow } from '../../model/activoImportRow';
import { ManualGridColumn, ManualGridImportFn } from '../../model/manualGrid';
import { ManualGridComponent } from '../../shared/manual-grid/manual-grid.component';

type ModoCarga = 'excel' | 'manual';
type EntidadCarga = 'activo' | 'proveedor' | 'orden' | 'repuesto' | 'ubicacion' | 'tipoActivo';

@Component({
  selector: 'app-carga-masiva',
  standalone: true,
  imports: [CommonModule, FormsModule, ManualGridComponent],
  templateUrl: './carga-masiva.component.html',
  styleUrl: './carga-masiva.component.css'
})
export class CargaMasivaComponent implements OnInit {

  cargaMasivaService = inject(CargaMasivaService);
  tipoActivoService = inject(TipoActivoService);
  ubicacionService = inject(UbicacionService);
  proveedorService = inject(ProveedorService);
  activoService = inject(ActivoService);

  fileNameActivo: string | null = null;
  fileNameProveedor: string | null = null;
  fileNameOrden: string | null = null;
  fileNameRepuesto: string | null = null;
  fileNameUbicacion: string | null = null;
  fileNameTipoActivo: string | null = null;
  progressActivo = 0;
  progressProveedor = 0;
  progressOrden = 0;
  progressRepuesto = 0;
  progressUbicacion = 0;
  progressTipoActivo = 0;
  estadoActivo = '';
  estadoProveedor = '';
  estadoOrden = '';
  estadoRepuesto = '';
  estadoUbicacion = '';
  estadoTipoActivo = '';
  interval: any;
  mensaje = '';
  mensajeActivo = '';
  mensajeProveedor = '';
  mensajeOrden = '';
  mensajeRepuesto = '';
  mensajeUbicacion = '';
  mensajeTipoActivo = '';

  // ==========================================================
  // 🔥 Ingreso en tiempo real (grilla tipo planilla) por entidad
  // ==========================================================

  // Modo actual (Excel / manual) de cada una de las 6 cargas masivas
  modos: Record<EntidadCarga, ModoCarga> = {
    activo: 'excel',
    proveedor: 'excel',
    orden: 'excel',
    repuesto: 'excel',
    ubicacion: 'excel',
    tipoActivo: 'excel'
  };

  cambiarModo(entidad: EntidadCarga, modo: ModoCarga) {
    this.modos[entidad] = modo;
  }

  // Catálogos usados como autocompletado (datalist) en las grillas manuales
  tipoActivos: TipoActivo[] = [];
  ubicaciones: Ubicacion[] = [];
  proveedores: Proveedor[] = [];
  activos: Activo[] = [];

  // ---- Opciones estáticas de enums del backend ----
  private opcionesTipoProveedor = [
    { value: 'INTERNO', label: 'Interno' },
    { value: 'EXTERNO', label: 'Externo' }
  ];

  private opcionesTipoMantenimiento = [
    { value: 'PREVENTIVO', label: 'Preventivo' },
    { value: 'CORRECTIVO', label: 'Correctivo' },
    { value: 'PREDICTIVO', label: 'Predictivo' }
  ];

  private opcionesEstadoOrden = [
    { value: 'PENDIENTE', label: 'Pendiente' },
    { value: 'PROGRAMADA', label: 'Programada' },
    { value: 'EN_EJECUCION', label: 'En ejecución' },
    { value: 'PRE_COMPLETADA', label: 'Pre completada' },
    { value: 'COMPLETADA', label: 'Completada' },
    { value: 'CANCELADA', label: 'Cancelada' },
    { value: 'ATRASADA', label: 'Atrasada' }
  ];

  private opcionesTipoRepuesto = [
    { value: 'REPUESTO', label: 'Repuesto' },
    { value: 'FUNGIBLE', label: 'Fungible' }
  ];

  // ---- Columnas de cada grilla ----
  //
  // ⚠️ IMPORTANTE: columnasActivo y columnasOrden dependen de catálogos que
  // se cargan por HTTP (tipos de activo, ubicaciones, proveedores, activos),
  // así que se recalculan solo cuando esos catálogos llegan (ver ngOnInit).
  // A propósito NO son "getters": un getter usado en un binding de plantilla
  // (`[columns]="columnasActivo"`) se vuelve a ejecutar en CADA ciclo de
  // detección de cambios de Angular (cada click, cada tecla, cada timer),
  // devolviendo un arreglo y objetos nuevos cada vez. Angular entonces ve
  // "columnas distintas" en cada revisión y destruye y reconstruye toda la
  // tabla (encabezados, celdas, inputs, datalists) constantemente — eso era
  // lo que causaba la demora/lentitud al entrar a "Ingresar manualmente".
  // Con un campo normal, el arreglo se crea una sola vez (o cuando cambia
  // el catálogo real) y Angular reutiliza el DOM entre ciclos.

  columnasActivo: ManualGridColumn[] = [];
  columnasOrden: ManualGridColumn[] = [];

  private construirColumnasActivo(): ManualGridColumn[] {
    return [
      { key: 'codigoInterno', label: 'Código*' },
      { key: 'nombre', label: 'Nombre*' },
      { key: 'descripcion', label: 'Descripción' },
      { key: 'tipoActivoNombre', label: 'Tipo Activo*', options: this.tipoActivos.map(t => ({ value: t.nombre, label: t.nombre })) },
      { key: 'marca', label: 'Marca' },
      { key: 'modelo', label: 'Modelo' },
      { key: 'numeroSerie', label: 'N° Serie' },
      { key: 'fechaAdquisicion', label: 'F. Adquisición*', type: 'date' },
      { key: 'valorAdquisicion', label: 'Valor Adq.*', type: 'number' },
      { key: 'valorResidual', label: 'Valor Residual', type: 'number' },
      { key: 'vidaUtilMeses', label: 'Vida Útil (meses)', type: 'number' },
      { key: 'ubicacionNombre', label: 'Ubicación*', options: this.ubicaciones.map(u => ({ value: u.nombre, label: u.nombre })) },
      { key: 'proveedorRut', label: 'Proveedor (RUT)*', options: this.proveedores.map(p => ({ value: p.rut, label: p.nombre })) },
      { key: 'cuentaContable', label: 'Cuenta Contable' }
    ];
  }

  private construirColumnasOrden(): ManualGridColumn[] {
    return [
      { key: 'titulo', label: 'Título*' },
      { key: 'fechaProgramada', label: 'F. Programada', type: 'date' },
      { key: 'horaProgramada', label: 'Hora Programada', type: 'time' },
      { key: 'duracionMinutos', label: 'Duración (min)', type: 'number' },
      { key: 'tipoMantenimiento', label: 'Tipo Mantención', options: this.opcionesTipoMantenimiento },
      { key: 'estado', label: 'Estado', options: this.opcionesEstadoOrden },
      { key: 'activoNombre', label: 'Activo', options: this.activos.map(a => ({ value: a.nombre, label: a.nombre })) },
      { key: 'proveedorRut', label: 'Proveedor (RUT)', options: this.proveedores.map(p => ({ value: p.rut, label: p.nombre })) },
      { key: 'valorHoraProveedor', label: 'Valor Hora Prov.*', type: 'number' },
      // 🔥 Se calculan solos: no son editables (ver ManualGridColumn.calcular)
      {
        key: 'horasEstimadasProveedor',
        label: 'Horas Estimadas (auto)',
        type: 'number',
        readonly: true,
        calcular: (fila) => this.calcularHorasEstimadas(fila.duracionMinutos)
      },
      {
        key: 'costoManoObraEstimadasProveedor',
        label: 'Costo M.O. Estimado (auto)',
        type: 'number',
        readonly: true,
        calcular: (fila) => this.calcularCostoManoObra(fila.valorHoraProveedor, fila.horasEstimadasProveedor)
      },
      { key: 'observaciones', label: 'Observaciones' }
    ];
  }

  // Duración (minutos) → horas estimadas
  private calcularHorasEstimadas(duracionMinutos: any): number | null {

    const minutos = Number(duracionMinutos);

    if (!minutos || minutos <= 0) return null;

    return Math.round((minutos / 60) * 100) / 100;
  }

  // Valor hora proveedor × horas estimadas → costo mano de obra estimado
  private calcularCostoManoObra(valorHoraProveedor: any, horasEstimadasProveedor: any): number | null {

    const valorHora = Number(valorHoraProveedor);
    const horas = Number(horasEstimadasProveedor);

    if (!valorHora || valorHora <= 0 || !horas || horas <= 0) return null;

    return Math.round(valorHora * horas * 100) / 100;
  }

  readonly columnasProveedor: ManualGridColumn[] = [
    { key: 'nombre', label: 'Nombre*' },
    { key: 'rut', label: 'RUT' },
    { key: 'contacto', label: 'Contacto' },
    { key: 'telefono', label: 'Teléfono' },
    { key: 'email', label: 'Email' },
    { key: 'tipoProveedor', label: 'Tipo', options: this.opcionesTipoProveedor }
  ];

  readonly columnasRepuesto: ManualGridColumn[] = [
    { key: 'codigo', label: 'Código*' },
    { key: 'nombre', label: 'Nombre*' },
    { key: 'descripcion', label: 'Descripción' },
    { key: 'costo', label: 'Costo*', type: 'number' },
    { key: 'stockActual', label: 'Stock Actual*', type: 'number' },
    { key: 'stockMinimo', label: 'Stock Mínimo*', type: 'number' },
    { key: 'cuentaContable', label: 'Cuenta Contable' },
    { key: 'tipoRepuesto', label: 'Tipo', options: this.opcionesTipoRepuesto }
  ];

  readonly columnasUbicacion: ManualGridColumn[] = [
    { key: 'nombre', label: 'Nombre*' },
    { key: 'descripcion', label: 'Descripción' },
    { key: 'direccion', label: 'Dirección' }
  ];

  readonly columnasTipoActivo: ManualGridColumn[] = [
    { key: 'nombre', label: 'Nombre*' },
    { key: 'descripcion', label: 'Descripción' },
    { key: 'vidaUtilReferencialMeses', label: 'Vida Útil Referencial (meses)', type: 'number' }
  ];

  // ---- Funciones de importación (adaptadores hacia el backend) ----

  importarActivoFn: ManualGridImportFn = (filas) =>
    this.cargaMasivaService.importarActivosManual(filas as ActivoImportRow[]).pipe(
      map(res => ({
        total: res.total,
        exitosos: res.exitosos,
        fallidos: res.fallidos,
        resultados: res.resultados.map(r => ({
          fila: r.fila,
          exito: r.exito,
          mensaje: r.mensaje,
          referencia: r.codigoInterno
        }))
      }))
    );

  importarProveedorFn: ManualGridImportFn = (filas) => this.cargaMasivaService.importarProveedoresManual(filas);
  importarOrdenFn: ManualGridImportFn = (filas) => this.cargaMasivaService.importarOrdenesManual(filas);
  importarRepuestoFn: ManualGridImportFn = (filas) => this.cargaMasivaService.importarRepuestosManual(filas);
  importarUbicacionFn: ManualGridImportFn = (filas) => this.cargaMasivaService.importarUbicacionesManual(filas);
  importarTipoActivoFn: ManualGridImportFn = (filas) => this.cargaMasivaService.importarTiposActivoManual(filas);

  ngOnInit() {
    // Columnas iniciales (sin opciones de catálogo todavía, se completan
    // apenas responde cada servicio más abajo).
    this.columnasActivo = this.construirColumnasActivo();
    this.columnasOrden = this.construirColumnasOrden();

    this.tipoActivoService.getTipoActivoCombo().subscribe({
      next: (data) => {
        this.tipoActivos = data.content;
        this.columnasActivo = this.construirColumnasActivo();
      },
      error: () => {}
    });

    this.ubicacionService.getUbicacionCombo().subscribe({
      next: (data) => {
        this.ubicaciones = data.content;
        this.columnasActivo = this.construirColumnasActivo();
      },
      error: () => {}
    });

    this.proveedorService.getProveedorCombo().subscribe({
      next: (data) => {
        this.proveedores = data.content;
        this.columnasActivo = this.construirColumnasActivo();
        this.columnasOrden = this.construirColumnasOrden();
      },
      error: () => {}
    });

    this.activoService.getActivoCombo().subscribe({
      next: (data) => {
        this.activos = data.content;
        this.columnasOrden = this.construirColumnasOrden();
      },
      error: () => {}
    });
  }

  onFileSelected(event: Event, archivo: String) {
    const input = event.target as HTMLInputElement;

    if (!input.files || input.files.length === 0) {
      return;
    }

    const file = input.files[0];

    if (archivo === "activo"){
      this.fileNameActivo = file.name;
      this.subir(file, archivo);
    } else if (archivo === "orden"){
      this.fileNameOrden = file.name;
      this.subir(file, archivo);
    } else if (archivo === "proveedor"){
      this.fileNameProveedor = file.name;
      this.subir(file, archivo);
    } else if (archivo === "repuesto"){
      this.fileNameRepuesto = file.name;
      this.subir(file, archivo);
    } else if (archivo === "ubicacion"){
      this.fileNameUbicacion = file.name;
      this.subir(file, archivo);
    } else if (archivo === "tipoActivo"){
      this.fileNameTipoActivo = file.name;
      this.subir(file, archivo);
    }

    input.value = '';
  }

  subir(file: File, archivo: String) {

    this.cargaMasivaService.upload(file, archivo).subscribe(res => {

      const jobId = res.jobId; // 👈 ahora viene como objeto JSON

      this.interval = setInterval(() => {

      this.cargaMasivaService.getProgress(jobId).subscribe(p => {

        // 🔒 El backend puede responder 200 con cuerpo vacío/null para este
        // jobId (por ejemplo, si el servidor se reinició mientras la carga
        // estaba en curso y el progreso en memoria se perdió). Sin este
        // guard, "p.estado" de más abajo rompía con
        // "Cannot read properties of null (reading 'estado')". Se ignora
        // este tick del polling y se sigue esperando el próximo.
        if (!p) {
          return;
        }

        console.log(p.estado);

      if (p.estado === 'COMPLETADO' || p.estado === 'COMPLETADO_CON_ERRORES' || p.estado === 'ERROR') {
        clearInterval(this.interval);

        switch (p.estado) {
          case 'ERROR':
            this.mensaje = '❌ Error en el proceso de carga';
            break;

          case 'COMPLETADO_CON_ERRORES':
            this.mensaje = '❌ Proceso de carga terminado con errores';
            console.log('Errores:', p.erroresDetalle);
            break;
        }
      }

      if (archivo === "activo"){
        this.mensajeActivo = this.mensaje;
        this.estadoActivo = p.estado;
        this.progressActivo = Math.round((p.procesados / p.total) * 100);
      } else if (archivo === "proveedor"){
        this.mensajeProveedor = this.mensaje;
        this.estadoProveedor = p.estado;
        this.progressProveedor = Math.round((p.procesados / p.total) * 100);
      } else if (archivo === "orden"){
        this.mensajeOrden = this.mensaje;
        this.estadoOrden = p.estado;
        this.progressOrden = Math.round((p.procesados / p.total) * 100);
      } else if (archivo === "repuesto"){
        this.mensajeRepuesto = this.mensaje;
        this.estadoRepuesto = p.estado;
        this.progressRepuesto = Math.round((p.procesados / p.total) * 100);
      } else if (archivo === "ubicacion"){
        this.mensajeUbicacion = this.mensaje;
        this.estadoUbicacion = p.estado;
        this.progressUbicacion = Math.round((p.procesados / p.total) * 100);
      } else if (archivo === "tipoActivo"){
        this.mensajeTipoActivo = this.mensaje;
        this.estadoTipoActivo = p.estado;
        this.progressTipoActivo = Math.round((p.procesados / p.total) * 100);
      }



    });

    }, 1000);

    });
  }

  reset(archivo: String) {

    if (archivo === "activo"){
      this.fileNameActivo = null;
      this.progressActivo = 0;
      this.mensajeActivo = '';
    } else if (archivo === "proveedor"){
      this.fileNameProveedor = null;
      this.progressProveedor = 0;
      this.mensajeProveedor = '';
    } else if (archivo === "orden"){
      this.fileNameOrden = null;
      this.progressOrden = 0;
      this.mensajeOrden = '';
    } else if (archivo === "repuesto"){
      this.fileNameRepuesto = null;
      this.progressRepuesto = 0;
      this.mensajeRepuesto = '';
    } else if (archivo === "ubicacion"){
      this.fileNameUbicacion = null;
      this.progressUbicacion = 0;
      this.mensajeUbicacion = '';
    } else if (archivo === "tipoActivo"){
      this.fileNameTipoActivo = null;
      this.progressTipoActivo = 0;
      this.mensajeTipoActivo = '';
    }
    this.mensaje = '';

  }

}
