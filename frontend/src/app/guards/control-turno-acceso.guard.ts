import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

// 🔐 Roles que participan de Control de Turno (igual que
// ROLES_CONTROL_TURNO en app.routes.ts / sidebar.component.ts) --
// duplicado aca a proposito, en vez de importarlo desde app.routes.ts,
// para no crear una dependencia circular entre el guard y las rutas.
const ROLES_CONTROL_TURNO = ['SUPER_ADMIN', 'ADMIN_EMPRESA', 'JEFE_MANTENIMIENTO', 'TECNICO'];

/**
 * Caso especial de la ruta 'controlTurno': el sidebar la muestra con
 * *ngIf="mostrarControlTurno" que combina ROL (ROLES_CONTROL_TURNO) CON
 * el flag de empresa Empresa.controlTurnoHabilitado -- algo que
 * `data.roles` + `roleGuard` no puede expresar solo (ese guard no
 * conoce flags de empresa). Este guard replica esa misma condicion a
 * nivel de ruta. Mismo patron que escanear-acceso.guard.ts.
 *
 * SUPER_ADMIN pasa siempre, igual que con codigoQrHabilitado/
 * codigoEan13Habilitado (ver escanear-acceso.guard.ts): puede
 * administrar/revisar el modulo aunque la empresa todavia no lo tenga
 * habilitado.
 */
export const controlTurnoAccesoGuard: CanActivateFn = () => {

  const auth = inject(AuthService);
  const router = inject(Router);

  const rolPermitido = ROLES_CONTROL_TURNO.includes(auth.getUserRole() ?? '');
  const permitido = auth.isAdmin() || (rolPermitido && !!auth.getControlTurnoHabilitado());

  if (permitido) {
    return true;
  }

  router.navigate(['/inicio/calendario']);
  return false;
};
