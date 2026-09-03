import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class RespaldoService {

  private apiUrl = environment.apiUrl;

  http = inject(HttpClient);

  // 🔒 Respaldo de TODA la base (todas las empresas), solo SUPER_ADMIN.
  // Ver RespaldoGeneralService en el backend.
  descargarBackupGeneral(): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/public/respaldo/general`, { responseType: 'blob' });
  }
}
