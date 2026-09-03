import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Empresa } from '../model/empresa';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class EmpresaService {

  private apiUrl = environment.apiUrl;

  http = inject(HttpClient);

  getAll(): Observable<Empresa[]> {
    return this.http.get<Empresa[]>(`${this.apiUrl}/public/empresas`);
  }

  create(empresa: Empresa, flag: number): Observable<Empresa> {
    if (flag == 0)
      return this.http.post<Empresa>(`${this.apiUrl}/public/empresas`, empresa);
    else
       return this.http.post<Empresa>(`${this.apiUrl}/public/empresas/onboarding`, empresa);
  }

  update(id: number, empresa: Empresa): Observable<Empresa> {
    return this.http.put<Empresa>(`${this.apiUrl}/public/empresas/${id}`, empresa);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/public/empresas/${id}`);
  }

  subirLogo(id: number, archivo: File): Observable<Empresa> {
    const formData = new FormData();
    formData.append('file', archivo);

    return this.http.post<Empresa>(`${this.apiUrl}/public/empresas/${id}/logo`, formData);
  }

  // 🔒 Respaldo puntual de una sola empresa (solo SUPER_ADMIN, ver
  // EmpresaBackupService): .zip con un .csv por tabla filtrado a esa
  // empresa.
  descargarBackup(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/public/empresas/${id}/backup`, { responseType: 'blob' });
  }

  // 🔥 No requiere autenticación (GET público, ver EmpresaController):
  // se puede usar directo en un <img [src]>. Se le agrega un query
  // param con el timestamp de subida (o Date.now() como fallback) para
  // invalidar el cache del navegador cuando se reemplaza el logo.
  getLogoUrl(id: number, cacheBust?: number): string {
    return `${this.apiUrl}/public/empresas/${id}/logo?v=${cacheBust ?? Date.now()}`;
  }

}
