import { inject, Injectable } from '@angular/core';
import { environment } from '../../environments/environment';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ActivoImportRow, ActivoImportResult } from '../model/activoImportRow';
import { ManualGridBatchResult } from '../model/manualGrid';

@Injectable({
  providedIn: 'root'
})
export class CargaMasivaService {

  private apiUrl = environment.apiUrl;
  private http = inject(HttpClient);

  upload(file: File, archivo: String) {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post<any>(`${this.apiUrl}/import/${archivo}`, formData);
  }

  getProgress(jobId: string) {
    return this.http.get<any>(`${this.apiUrl}/import/progress/${jobId}`);
  }

  // ==========================================================
  // 🔥 Ingreso en tiempo real (grilla tipo planilla) por entidad
  // ==========================================================

  importarActivosManual(filas: ActivoImportRow[]): Observable<ActivoImportResult> {
    console.log("Filas: " + filas)
    return this.http.post<ActivoImportResult>(`${this.apiUrl}/import/activo/manual`, filas);
  }

  importarProveedoresManual(filas: any[]): Observable<ManualGridBatchResult> {
    return this.http.post<ManualGridBatchResult>(`${this.apiUrl}/import/proveedor/manual`, filas);
  }

  importarOrdenesManual(filas: any[]): Observable<ManualGridBatchResult> {
    return this.http.post<ManualGridBatchResult>(`${this.apiUrl}/import/orden/manual`, filas);
  }

  importarRepuestosManual(filas: any[]): Observable<ManualGridBatchResult> {
    return this.http.post<ManualGridBatchResult>(`${this.apiUrl}/import/repuesto/manual`, filas);
  }

  importarUbicacionesManual(filas: any[]): Observable<ManualGridBatchResult> {
    return this.http.post<ManualGridBatchResult>(`${this.apiUrl}/import/ubicacion/manual`, filas);
  }

  importarTiposActivoManual(filas: any[]): Observable<ManualGridBatchResult> {
    return this.http.post<ManualGridBatchResult>(`${this.apiUrl}/import/tipoActivo/manual`, filas);
  }
}
