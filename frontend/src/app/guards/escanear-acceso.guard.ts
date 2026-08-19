import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Caso especial de la ruta 'escanear': el sidebar la muestra con
 * *ngIf="esAdmin || codigoQrHabilitado || codigoEan13Habilitado" -- mezcla
 * rol (SUPER_ADMIN) con flags de empresa (QR/EAN13 habilitado), algo que
 * `data.roles` + `roleGuard` no puede expresar. Este guard replica esa
 * misma condicion exacta a nivel de ruta. Coexiste con `canActivateChild`
 * del padre (ambos deben cumplirse).
 */
export const escanearAccesoGuard: CanActivateFn = () => {

  const auth = inject(AuthService);
  const router = inject(Router);

  const permitido =
    auth.isAdmin() ||
    !!auth.getCodigoQrHabilitado() ||
    !!auth.getCodigoEan13Habilitado();

  if (permitido) {
    return true;
  }

  router.navigate(['/inicio/calendario']);
  return false;
};
