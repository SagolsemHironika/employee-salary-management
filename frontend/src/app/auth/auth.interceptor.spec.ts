import { HttpRequest } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

describe('authInterceptor', () => {
  function runInterceptor(token: string | null) {
    TestBed.configureTestingModule({
      providers: [{ provide: AuthService, useValue: { token } }]
    });
    const req = new HttpRequest('GET', '/employees');
    const next = vi.fn().mockReturnValue(of('handled'));

    TestBed.runInInjectionContext(() => authInterceptor(req, next));

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
});
