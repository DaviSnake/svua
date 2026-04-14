import { HttpClient } from '@angular/common/http';
import { inject, Inject, Injectable } from '@angular/core';
import { LoginRequest } from '../auth/models/login-request';
import { LoginResponse } from '../auth/models/login-response';
import { jwtDecode } from 'jwt-decode';
import { BehaviorSubject } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private apiUrl = environment.apiUrl;
  private http = inject(HttpClient);
  private refreshTimeout: any;

  private userSubject = new BehaviorSubject<any>(null);
  user$ = this.userSubject.asObservable();

  getTokenExpiration(token: string): number {
    const decoded: any = jwtDecode(token);
    return decoded.exp * 1000; // a milisegundos
  }

  getUserRole(): string | null {
    const token = localStorage.getItem('token');
    if (!token) return null;

    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.rol;
  }

  init() {
    this.setUserFromToken();
  }

  startRefreshTimer() {
    const token = localStorage.getItem('token');

    if (!token) return;

    const expires = this.getTokenExpiration(token);
    const timeout = expires - Date.now() - (2 * 60 * 1000); // 2 min antes

    this.refreshTimeout = setTimeout(() => {
      this.getRefreshToken();
    }, timeout);
  }

  setUserFromToken() {
    const token = localStorage.getItem('token');

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

  guardarToken(token: string) {
    localStorage.setItem('token', token);
  }

  guardarRefreshToken(refreshToken: string) {
    localStorage.setItem('refreshToken', refreshToken);
  }

  getToken() {
    return localStorage.getItem('token');
  }

  getRefreshToken() {
    const refreshToken = localStorage.getItem('refreshToken');

    this.http.post<any>(`${this.apiUrl}/auth/refresh`, {
      refreshToken
    }).subscribe({
      next: (res) => {
        localStorage.setItem('token', res.accessToken);
        localStorage.setItem('refreshToken', res.refreshToken);
        this.init();

        // 🔁 reiniciar timer
        this.startRefreshTimer();
      },
      error: () => {
        this.logout();
      }
    });
  }

  logout() {
    return this.http.post(`${this.apiUrl}/auth/logout`, null);
  }

  isLogged(): boolean {
    return !!this.getToken();
  }

  getUser() {
    const token = localStorage.getItem('token');

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
