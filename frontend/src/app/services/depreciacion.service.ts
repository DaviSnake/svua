import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

// 🔥 Backfill de depreciación acelerada tributaria (SII) para activos
// que quedaron sin ese cronograma (creados antes de que existiera, o
// cargados por Excel). Ver DepreciacionController en el backend.
@Injectable({
  providedIn: 'root'
})
export class DepreciacionService {

  private apiUrl = environment.apiUrl;
  private http = inject(HttpClient);

  // Genera la depreciación acelerada para todos los activos de la
  // empresa actual que aún no la tienen. Devuelve la cantidad procesada.
  generarAceleradaFaltante(): Observable<number> {
    return this.http.post<number>(
      `${this.apiUrl}/depreciacion/acelerada/generar-faltantes`,
      null
    );
  }

  // Genera la depreciación acelerada de un activo puntual. El backend
  // rechaza la llamada (400) si el activo ya la tenía calculada.
  generarAceleradaPorActivo(activoId: number): Observable<void> {
    return this.http.post<void>(
      `${this.apiUrl}/depreciacion/acelerada/generar/${activoId}`,
      null
    );
  }
}
