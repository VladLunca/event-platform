import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap, finalize } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { AuthResponse, TokenPayload, Role } from '../models/user.model';

interface RawJwtPayload {
  sub: string;
  role: Role;
  exp: number;
  iat: number;
}

@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly TOKEN_KEY = 'auth_token';
  private readonly apiUrl = '/api/auth';

  currentUser = signal<TokenPayload | null>(this.loadUserFromStorage());

  constructor(private http: HttpClient) {}

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, { email, password }).pipe(
      tap(response => {
        localStorage.setItem(this.TOKEN_KEY, response.token);
        this.currentUser.set(this.parseToken(response.token));
      })
    );
  }

  logout(): Observable<unknown> {
    return this.http.post(`${this.apiUrl}/logout`, {}).pipe(
      finalize(() => this.clearSession())
    );
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  isLoggedIn(): boolean {
    const user = this.currentUser();
    if (!user) return false;
    return user.expiresAt * 1000 > Date.now();
  }

  hasRole(role: Role): boolean {
    return this.currentUser()?.role === role;
  }

  private clearSession(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    this.currentUser.set(null);
  }

  private loadUserFromStorage(): TokenPayload | null {
    const token = localStorage.getItem(this.TOKEN_KEY);
    if (!token) return null;
    try {
      const payload = this.parseToken(token);
      if (payload.expiresAt * 1000 < Date.now()) {
        localStorage.removeItem(this.TOKEN_KEY);
        return null;
      }
      return payload;
    } catch {
      return null;
    }
  }

  private parseToken(token: string): TokenPayload {
    const raw = JSON.parse(atob(token.split('.')[1])) as RawJwtPayload;
    return {
      sub: raw.sub,
      role: raw.role,
      expiresAt: raw.exp,
      issuedAt: raw.iat
    };
  }
  createUser(data: { email: string; password: string; role: string }): Observable<unknown> {
    return this.http.post(`${this.apiUrl}/users`, data);
  }

}
