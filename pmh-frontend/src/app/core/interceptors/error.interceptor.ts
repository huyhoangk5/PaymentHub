import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !req.url.includes('/api/auth/')) {
        return authService.refreshToken().pipe(
          switchMap((res) => {
            const newAccessToken = res.data?.accessToken;
            if (!newAccessToken) {
              authService.logout();
              router.navigate(['/login']);
              return throwError(() => error);
            }
            const newReq = req.clone({
              setHeaders: {
                Authorization: `Bearer ${newAccessToken}`
              }
            });
            return next(newReq);
          }),
          catchError((refreshErr) => {
            authService.logout();
            router.navigate(['/login']);
            return throwError(() => refreshErr);
          })
        );
      } else if (error.status === 403) {
        console.error('Permission denied: 403 Forbidden');
      } else if (error.status === 500) {
        console.error('System error 500:', error.message);
      }

      return throwError(() => error);
    })
  );
};
