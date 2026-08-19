import { HttpClient } from '@angular/common/http';
import { Injectable, computed, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { LoginRequest, LoginResponse } from './auth.model';

const TOKEN_STORAGE_KEY = 'salary_auth_token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly tokenSignal = signal<string | null>(localStorage.getItem(TOKEN_STORAGE_KEY));
  readonly isAuthenticated = computed(() => this.tokenSignal() !== null);

  constructor(private readonly http: HttpClient) {}

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/auth/login', request).pipe(
      tap((response) => {
        localStorage.setItem(TOKEN_STORAGE_KEY, response.token);
        this.tokenSignal.set(response.token);
      })
    );
  }

  logout(): void {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    this.tokenSignal.set(null);
  }

  get token(): string | null {
    return this.tokenSignal();
  }
}
