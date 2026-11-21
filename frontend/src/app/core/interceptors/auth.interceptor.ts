import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AuthService } from '../auth/auth.service';
import { SKIP_AUTH } from './auth-context';

/**
 * Adds the logged-in user's HTTP Basic credentials to every API request.
 * X-Requested-With makes Spring Security answer 401 without the WWW-Authenticate challenge,
 * so the browser does not show its own login popup.
 * A 401 on a normal request means the session is no longer valid: log out and go to login.
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const skipAuth = request.context.get(SKIP_AUTH);

  const headers: Record<string, string> = { 'X-Requested-With': 'XMLHttpRequest' };
  const authorization = skipAuth ? null : auth.authorizationHeader();
  if (authorization) {
    headers['Authorization'] = authorization;
  }

  return next(request.clone({ setHeaders: headers })).pipe(
    catchError((error: unknown) => {
      if (!skipAuth && error instanceof HttpErrorResponse && error.status === 401) {
        auth.logout();
        void router.navigate(['/login']);
      }
      return throwError(() => error);
    }),
  );
};
