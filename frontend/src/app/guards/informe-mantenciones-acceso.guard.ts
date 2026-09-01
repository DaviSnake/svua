import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

// 🔐 Roles que ven el Informe de Mantenciones (igual que data.roles en
// app.routes.ts / sidebar.component.ts) -- duplicado aca a proposito,
// en vez de importarlo desde app.routes.ts, para no crear una
// dependencia circular entre el guard y las rutas.
const ROLES_INFORME_MANTENCIONES = ['SUPER_ADMIN', 'ADMIN_EMPRESA'];

/**
 * Caso especial de la ruta 'informeMantenciones': el sidebar la muestra
 * combinando ROL (ROLES_INFORME_MANTENCIONES) CON el flag de empresa
 * Empresa.informeMantencionesHabilitado -- algo que `data.roles` +
 * `roleGuard` no puede expresar solo (ese guard no conoce flags de
 * empresa). Este guard replica esa misma condicion a nivel de ruta.
 * Mismo patron que control-turno-acceso.guard.ts.
 *
 * SUPER_ADMIN pasa siempre, igual que con los demas flags de empresa:
 * puede administrar/revisar el informe aunque la empresa lo tenga
 * deshabilitado.
 */
export const informeMantencionesAccesoGuard: CanActivateFn = () => {

  const auth = inject(AuthService);
  const router = inject(Router);

  const rolPermitido = ROLES_INFORME_MANTENCIONES.includes(auth.getUserRole() ?? '');
  const permitido = auth.isAdmin() || (rolPermitido && !!auth.getInformeMantencionesHabilitado());

  if (permitido) {
    return true;
  }

  router.navigate(['/inicio/calendario']);
  return false;
};
