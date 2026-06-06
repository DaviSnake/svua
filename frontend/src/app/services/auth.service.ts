import { HttpClient } from '@angular/common/http';
import { inject, Inject, Injectable } from '@angular/core';
import { LoginRequest } from '../auth/models/login-request';
import { LoginResponse } from '../auth/models/login-response';
import { jwtDecode } from 'jwt-decode';
import { BehaviorSubject } from 'rxjs';
import { environment } from '../../environments/environment';
import { EmailResetRequest } from '../auth/models/email-reset-request';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private apiUrl = environment.apiUrl;
  private http = inject(HttpClient);
  private refreshTimeout?: ReturnType<typeof setTimeout>;

  private userSubject = new BehaviorSubject<any>(null);
  user$ = this.userSubject.asObservable();

  constructor() {

    window.addEventListener('beforeunload', () => {
      this.stopRefreshTimer();
    });

  }

  getTokenExpiration(token: string): number {
    const decoded: any = jwtDecode(token);
    return decoded.exp * 1000; // a milisegundos
  }

  getUserRole(): string | null {
    const token = sessionStorage.getItem('token');
    if (!token) return null;

    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.rol;
  }

  getEmpresaId(): number | null {
    const token = sessionStorage.getItem('token');
    if (!token) return null;

    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.empresaId;
  }

  init() {
    this.setUserFromToken();
  }

  startRefreshTimer() {

    // 🔥 detener timer anterior
    this.stopRefreshTimer();

    const token = sessionStorage.getItem('token');

    if (!token) return;

    const expires = this.getTokenExpiration(token);
    const timeout = expires - Date.now() - (2 * 60 * 1000); // 2 min antes

    // token expirado
    if (timeout <= 0) {
      this.getRefreshToken();
      return;
    }

    this.refreshTimeout = setTimeout(() => {
      this.getRefreshToken();
    }, timeout);
  }

  private stopRefreshTimer() {
    if (this.refreshTimeout) {
      clearTimeout(this.refreshTimeout);
      this.refreshTimeout = undefined;
    }
  }

  setUserFromToken() {
    const token = sessionStorage.getItem('token');

    if (!token) return;

  try {
      const user = jwtDecode(token);
      this.userSubject.next(user);
    } catch (e) {
      console.error('Token inválido');
      this.logout();
    }
  }

  login(data: LoginRequest) {
    return this.http.post<LoginResponse>(`${this.apiUrl}/auth/login`, data);
  }

  register(data: LoginRequest) {
    return this.http.post<LoginResponse>(`${this.apiUrl}/auth/login`, data);
  }

  forgotPassword(data: EmailResetRequest) {
    return this.http.post<any>(`${this.apiUrl}/auth/request-reset`, data );
  }

  guardarToken(token: string) {
    sessionStorage.setItem('token', token);
  }

  guardarRefreshToken(refreshToken: string) {
    sessionStorage.setItem('refreshToken', refreshToken);
  }

  getToken() {
    return sessionStorage.getItem('token');
  }

  getRefreshToken() {
    const refreshToken = sessionStorage.getItem('refreshToken');

    this.http.post<any>(`${this.apiUrl}/auth/refresh`, {
      refreshToken
    }).subscribe({
      next: (res) => {
        sessionStorage.setItem('token', res.accessToken);
        sessionStorage.setItem('refreshToken', res.refreshToken);
        this.init();

        // 🔁 reiniciar timer
        this.startRefreshTimer();
      },
      error: () => {
        this.logout();
      }
    });
  }

  validateToken(token: string) {
    return this.http.get(`${this.apiUrl}/auth/validate-token?token=${token}`);
  }

  resetPassword(token: string, password: string) {
    return this.http.post(`${this.apiUrl}/auth/reset-password`, { token, password });
  }

  logout() {
    this.stopRefreshTimer();

    this.userSubject.next(null);
    return this.http.post(`${this.apiUrl}/auth/logout`, null);
  }

  clearSession() {

    sessionStorage.clear();

    this.userSubject.next(null);

  }

  isLogged(): boolean {
    return !!this.getToken();
  }

  getUser() {
    const token = sessionStorage.getItem('token');

    if (!token) return null;

    return jwtDecode<any>(token);
  }

  isAdminEmpresa(): boolean {
    return this.getUserRole() === 'ADMIN_EMPRESA';
  }
  isAdmin(): boolean {
    return this.getUserRole() === 'SUPER_ADMIN';
  }
}
