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

}
