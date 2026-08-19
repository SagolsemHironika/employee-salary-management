import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { throwError, of } from 'rxjs';
import { Login } from './login';
import { AuthService } from '../auth.service';

describe('Login', () => {
  let authServiceMock: { login: ReturnType<typeof vi.fn> };
  let routerMock: { navigateByUrl: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    authServiceMock = { login: vi.fn() };
    routerMock = { navigateByUrl: vi.fn() };

    TestBed.configureTestingModule({
      imports: [Login],
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        { provide: Router, useValue: routerMock }
      ]
    });
  });

  it('does not submit while the form is invalid', () => {
    const fixture = TestBed.createComponent(Login);
    fixture.componentInstance.form.setValue({ email: '', password: '' });

    fixture.componentInstance.submit();

    expect(authServiceMock.login).not.toHaveBeenCalled();
  });

  it('navigates to the employee list on successful login', () => {
    authServiceMock.login.mockReturnValue(of({ token: 'jwt' }));
    const fixture = TestBed.createComponent(Login);
    fixture.componentInstance.form.setValue({ email: 'admin@acme.example', password: 'ChangeMe123!' });

    fixture.componentInstance.submit();

    expect(authServiceMock.login).toHaveBeenCalledWith({ email: 'admin@acme.example', password: 'ChangeMe123!' });
    expect(routerMock.navigateByUrl).toHaveBeenCalledWith('/');
    expect(fixture.componentInstance.errorMessage()).toBeNull();
  });

  it('shows an error message and does not navigate on failed login', () => {
    authServiceMock.login.mockReturnValue(throwError(() => new Error('unauthorized')));
    const fixture = TestBed.createComponent(Login);
    fixture.componentInstance.form.setValue({ email: 'admin@acme.example', password: 'wrong' });

    fixture.componentInstance.submit();

    expect(routerMock.navigateByUrl).not.toHaveBeenCalled();
    expect(fixture.componentInstance.errorMessage()).toBe('Invalid email or password');
    expect(fixture.componentInstance.submitting()).toBe(false);
  });
});
