import { inject, Injectable } from '@angular/core';
import { environment } from '../../environments/environment';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { LogArchivoResponse } from '../components/ver-logs/models/log-archivo.model';
import { Page } from '../shared/page';

// 🔥 Pantalla "Ver logs" (solo SUPER_ADMIN): lista y muestra el
// contenido de los .txt de error generados por las cargas masivas
// (ver ImportFileLogService en el backend).
@Injectable({
  providedIn: 'root'
})
export class LogArchivoService {

  private http = inject(HttpClient);

  listar(
    page: number,
    size: number,
    empresaId?: number
  ): Observable<Page<LogArchivoResponse>> {

    let params = new HttpParams()
      .set('page', page)
      .set('size', size);

    if (empresaId != null) {
      params = params.set('empresaId', empresaId);
    }

    return this.http.get<Page<LogArchivoResponse>>(
      `${environment.apiUrl}/logs`,
      { params }
    );
  }

  verArchivo(empresaId: number, nombreArchivo: string): Observable<string> {

    const params = new HttpParams()
      .set('empresaId', empresaId)
      .set('nombreArchivo', nombreArchivo);

    return this.http.get(
      `${environment.apiUrl}/logs/archivo`,
      { params, responseType: 'text' }
    );
  }
}
