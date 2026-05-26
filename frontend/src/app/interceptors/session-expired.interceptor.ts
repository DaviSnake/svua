import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

export const sessionExpiredInterceptor: HttpInterceptorFn = (req, next) => {

  const router = inject(Router);

  return next(req).pipe(
    catchError((error) => {

      if (error.status === 401 || error.status === 403) {

        sessionStorage.clear();
        router.navigate(['/login']);

        return throwError(() => new Error('Sesión expirada'));
      }

      return throwError(() => error);
    })
  );
};