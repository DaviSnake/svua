import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class NotificacionStateService {

  private actualizarCantidadSource =
    new BehaviorSubject<void>(undefined);

  actualizarCantidad$ =
    this.actualizarCantidadSource.asObservable();

  notificarActualizacion(): void {
    this.actualizarCantidadSource.next();
  }
}
