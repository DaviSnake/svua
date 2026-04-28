import { inject, Injectable } from '@angular/core';
import { environment } from '../../environments/environment';
import { HttpClient } from '@angular/common/http';

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
}
