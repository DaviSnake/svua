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

  // 🔥 empresaId opcional: solo lo usa el SUPER_ADMIN para ver el
  // calendario de una empresa distinta a la propia (el backend lo
  // ignora para el resto de los roles).
  listar(empresaId?: number) {

    let params = new HttpParams();

    if (empresaId != null) {
      params = params.set('empresaId', empresaId);
    }

    return this.http.get<OrdenMantencion[]>(
      `${this.apiUrl}/ordenes-mantenimiento`,
      { params }
    );
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

  // 🔥 permite adjuntar el checklist DESPUÉS de haber terminado la
  // ejecución sin él (dentro de las 24h de gracia que se avisan al
  // usuario en el modal de "Pre Finalizar mantención").
  subirChecklist(id: number, formData: FormData) {
    return this.http.post(`${this.apiUrl}/ordenes-mantenimiento/${id}/subirChecklist`, formData);
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
    estado?: string,
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

    if (estado) {
      params = params.set('estado', estado);
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
