import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * Antes: CUALQUIER 401 (de cualquier endpoint) cerraba la sesión de
 * inmediato. Eso significaba que, por ejemplo, un token que vencía a mitad
 * de una acción normal mandaba al usuario al login aunque el refresh token
 * todavía fuera válido.
 *
 * Ahora: ante un 401, primero se intenta renovar el token en silencio y
 * reintentar la MISMA petición con el token nuevo. La sesión solo se da
 * por terminada (aviso + logout) si ese refresh también falla — ahí sí el
 * refresh token venció o ya no es válido.
 */
export const sessionExpiredInterceptor: HttpInterceptorFn = (req, next) => {

  const authService = inject(AuthService);

  return next(req).pipe(
    catchError((error) => {

      if (error.status === 401) {

        // El propio /auth/refresh devolvió 401: el refresh token ya no
        // sirve. `AuthService.refrescarToken()` ya se encarga de mostrar
        // el aviso y cerrar sesión; acá solo se propaga el error, sin
        // reintentar (evita un loop).
        if (req.url.includes('/auth/refresh')) {
          return throwError(() => error);
        }

        // Cualquier otro endpoint: intentar renovar el token y reintentar
        // esta misma petición una vez con el token nuevo.
        return authService.refrescarToken().pipe(
          switchMap((res) => {
            const reintento = req.clone({
              setHeaders: { Authorization: `Bearer ${res.accessToken}` }
            });
            return next(reintento);
          }),
          // Si el refresh (o el reintento) también falla, se propaga el
          // 401 original. `refrescarToken()` ya mostró el aviso de sesión
          // expirada si correspondía.
          catchError(() => throwError(() => error))
        );
      }

      return throwError(() => error);
    })
  );
};
