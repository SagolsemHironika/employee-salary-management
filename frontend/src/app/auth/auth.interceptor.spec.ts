import { HttpErrorResponse, HttpRequest } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

describe('authInterceptor', () => {
  let authMock: { token: string | null; logout: ReturnType<typeof vi.fn> };
  let routerMock: { navigateByUrl: ReturnType<typeof vi.fn> };

  function runInterceptor(
    token: string | null,
    options: { url?: string; response?: ReturnType<typeof of> } = {}
  ) {
    authMock = { token, logout: vi.fn() };
    routerMock = { navigateByUrl: vi.fn() };

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authMock },
        { provide: Router, useValue: routerMock }
      ]
    });

    const req = new HttpRequest('GET', options.url ?? '/employees');
    const next = vi.fn().mockReturnValue(options.response ?? of('handled'));

    TestBed.runInInjectionContext(() =>
      // Subscribe so the catchError operator actually runs.
      authInterceptor(req, next).subscribe({ next: () => {}, error: () => {} })
    );

    return next;
  }

  it('attaches a Bearer authorization header when a token is present', () => {
    const next = runInterceptor('jwt-token');

    const forwardedRequest = next.mock.calls[0][0] as HttpRequest<unknown>;
    expect(forwardedRequest.headers.get('Authorization')).toBe('Bearer jwt-token');
  });

  it('forwards the request unchanged when there is no token', () => {
    const next = runInterceptor(null);

    const forwardedRequest = next.mock.calls[0][0] as HttpRequest<unknown>;
    expect(forwardedRequest.headers.has('Authorization')).toBe(false);
  });

  it('clears the session and redirects to login when a request is rejected as 401', () => {
    runInterceptor('expired-token', {
      response: throwError(() => new HttpErrorResponse({ status: 401 }))
    });

    expect(authMock.logout).toHaveBeenCalled();
    expect(routerMock.navigateByUrl).toHaveBeenCalledWith('/login');
  });

  it('leaves the session alone for non-401 failures', () => {
    runInterceptor('jwt-token', {
      response: throwError(() => new HttpErrorResponse({ status: 500 }))
    });

    expect(authMock.logout).not.toHaveBeenCalled();
    expect(routerMock.navigateByUrl).not.toHaveBeenCalled();
  });

  it('does not redirect when the login endpoint itself returns 401', () => {
    // A rejected sign-in is a wrong password, which the login screen reports;
    // bouncing the route there would wipe the error message the user needs.
    runInterceptor(null, {
      url: '/auth/login',
      response: throwError(() => new HttpErrorResponse({ status: 401 }))
    });

    expect(authMock.logout).not.toHaveBeenCalled();
    expect(routerMock.navigateByUrl).not.toHaveBeenCalled();
  });
});
