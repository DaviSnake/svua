import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const sessionExpiredInterceptor: HttpInterceptorFn = (req, next) => {

  const authService = inject(AuthService);

  return next(req).pipe(
    catchError((error) => {

      if (error.status === 401) {

        // La llamada a /auth/refresh ya maneja su propio 401 en
        // AuthService.getRefreshToken() (decide si reintentar o cerrar
        // sesión). Si este interceptor también reaccionara acá, un mismo
        // refresh fallido terminaba disparando el cierre de sesión dos
        // veces en paralelo.
        if (!req.url.includes('/auth/refresh')) {
          authService.sesionExpirada();
        }

        return throwError(() => new Error('Sesión expirada'));
      }

      if (error.status === 403) {
        return throwError(() => error);
      }

      return throwError(() => error);
    })
  );
};
