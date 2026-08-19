import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const token = authService.token;

  const request = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(request).pipe(
    catchError((error: unknown) => {
      // `isAuthenticated()` only asserts that a token exists, never that the
      // server still accepts it. Without this, an expired token leaves the app
      // looking signed in while every request 401s — the shell renders "Sign
      // out" and each screen sits empty forever with no way to recover but
      // manually clearing storage.
      //
      // A 401 from the login endpoint is just a wrong password; the login
      // component reports that itself and must not be bounced.
      const isLoginRequest = req.url.includes('/auth/login');
      if (error instanceof HttpErrorResponse && error.status === 401 && !isLoginRequest) {
        authService.logout();
        router.navigateByUrl('/login');
      }
      return throwError(() => error);
    })
  );
};
