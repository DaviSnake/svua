import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { OrdenMantencion } from '../model/ordenMantencion';
import { Observable, of } from 'rxjs';
import { environment } from '../../environments/environment';
import { OrdenResponse } from '../model/ordenResponse';
import { Page } from '../shared/page';
import { OrdenMantenimientoReporte } from '../model/ordenMantenimientoReporte';

@Injectable({
  providedIn: 'root'
})
export class OrdenMantencionService {

  private apiUrl = environment.apiUrl;
  private http = inject(HttpClient);

  listar() {
    //return of([] as Cita[]);
    return this.http.get<OrdenMantencion[]>(`${this.apiUrl}/ordenes-mantenimiento`);
  }

  crear(ordenMantencion: OrdenMantencion) {
    return this.http.post<OrdenMantencion>(`${this.apiUrl}/ordenes-mantenimiento`, ordenMantencion);
  }

  iniciar(id: number) {
    return this.http.put<OrdenResponse>(`${this.apiUrl}/ordenes-mantenimiento/${id}/ejecutar`, {});
  }

  detener(id: number) {
    return this.http.put<OrdenResponse>(`${this.apiUrl}/ordenes-mantenimiento/${id}/detener`, {});
  }

  detenerConArchivo(id: number, formData: FormData) {
    return this.http.post(`${this.apiUrl}/ordenes-mantenimiento/${id}/preDetenerConArchivo`, formData);
  }

  actualizar(id: number, ordenMantencion: OrdenMantencion) {
    return this.http.put(`${this.apiUrl}/ordenes-mantenimiento/${id}`, ordenMantencion);
  }

  reprogramar(id: number, fecha: Date, motivo: String) {
    const body = {
      nuevaFecha: this.formatLocalDateTime(fecha),
      motivo: motivo
    };
    return this.http.put(`${this.apiUrl}/ordenes-mantenimiento/${id}/reprogramar`, body);
  }

  cancelar(id: number, motivo: string, usuarioId: number) {
    return this.http.put(
      `${this.apiUrl}/ordenes-mantenimiento/${id}/cancelar?motivo=${motivo}&usuarioId=${usuarioId}`, {});
  }

  eliminar(id: number) {
    return this.http.delete(`${this.apiUrl}/ordenes-mantenimiento/${id}`);
  }

  private formatLocalDateTime(date: Date): string {
    const pad = (n: number) => n.toString().padStart(2, '0');

    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}` +
          `T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
  }

  getRiesgo(id: number) {
    return this.http.get(`${this.apiUrl}/activos/${id}/riesgo`);
  }

  verArchivo(id: number): Observable<Blob> {
  return this.http.get(
    `${environment.apiUrl}/ordenes-mantenimiento/${id}/archivo`,
    {
      responseType: 'blob'
    }
  );
}

  // 🔥 Informe de Mantenciones: historial paginado y filtrable de
  // órdenes COMPLETADAS, con el detalle de repuestos utilizados para el
  // comprobante (solo SUPER_ADMIN puede consultarlo).
  obtenerInformeMantenciones(
    page: number,
    size: number,
    usuario?: string,
    empresaId?: number,
    fecha?: string
  ): Observable<Page<OrdenMantenimientoReporte>> {

    let params = new HttpParams()
      .set('page', page)
      .set('size', size);

    if (usuario) {
      params = params.set('usuario', usuario);
    }

    if (empresaId != null) {
      params = params.set('empresaId', empresaId);
    }

    if (fecha) {
      params = params.set('fecha', fecha);
    }

    return this.http.get<Page<OrdenMantenimientoReporte>>(
      `${this.apiUrl}/ordenes-mantenimiento/informe`,
      { params }
    );
  }

}
