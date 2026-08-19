import { inject } from '@angular/core';
import { CanActivateChildFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Ruta segura de fallback cuando un usuario autenticado intenta acceder a
 * una seccion para la que no tiene el rol requerido. Coincide con el
 * destino del wildcard '**' en app.routes.ts, la unica seccion visible
 * para TODOS los roles logueados.
 */
const RUTA_SEGURA_POR_DEFECTO = '/inicio/calendario';

/**
 * Guard de ROL para las rutas hijas de 'inicio'. Se registra como
 * `canActivateChild` en la ruta padre (junto al `authGuard` ya existente
 * en `canActivate`), asi una sola declaracion cubre todos los children sin
 * repetir el guard en cada uno.
 *
 * 🔐 Antes de este guard, cualquier usuario autenticado (sin importar su
 * rol) podia navegar directo escribiendo la URL (ej. /inicio/empresa,
 * /inicio/configuracion) a secciones que el sidebar ya oculta segun rol
 * -- la UI las escondia, pero la ruta en si no estaba protegida. Este
 * guard replica EXACTAMENTE las mismas reglas que ya aplica el sidebar
 * hoy (defensa en profundidad, no una politica nueva).
 *
 * Lee `data.roles` de la ruta hija que el router esta por activar:
 *  - Si la ruta NO define `data.roles`, se permite el acceso a cualquier
 *    usuario logueado (fail-open intencional: `authGuard` ya garantiza
 *    login; esto solo restringe rutas que EXPLICITAMENTE declaren roles).
 *  - Si la define, permite el acceso solo si el rol del usuario
 *    (`AuthService.getUserRole()`, extraido del JWT) esta en esa lista.
 *  - Si no cumple, redirige a la ruta segura y bloquea la navegacion.
 */
export const roleGuard: CanActivateChildFn = (childRoute) => {

  const auth = inject(AuthService);
  const router = inject(Router);

  const rolesPermitidos = childRoute.data?.['roles'] as string[] | undefined;

  if (!rolesPermitidos || rolesPermitidos.length === 0) {
    return true;
  }

  const rolActual = auth.getUserRole();

  if (rolActual && rolesPermitidos.includes(rolActual)) {
    return true;
  }

  router.navigate([RUTA_SEGURA_POR_DEFECTO]);
  return false;
};
