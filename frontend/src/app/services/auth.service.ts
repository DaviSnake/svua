import { HttpClient } from '@angular/common/http';
import { inject, Inject, Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { LoginRequest } from '../auth/models/login-request';
import { LoginResponse } from '../auth/models/login-response';
import { jwtDecode } from 'jwt-decode';
import { BehaviorSubject } from 'rxjs';
import { environment } from '../../environments/environment';
import { EmailResetRequest } from '../auth/models/email-reset-request';
import Swal from 'sweetalert2';

// Mensajes que se sincronizan entre pestañas del mismo navegador a través
// de BroadcastChannel (ver comentario más abajo, junto a `canal`).
type MensajeAuthBroadcast =
  | { type: 'tokens'; accessToken: string; refreshToken: string }
  | { type: 'logout' };

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private apiUrl = environment.apiUrl;
  private http = inject(HttpClient);
  private router = inject(Router);
  private refreshTimeout?: ReturnType<typeof setTimeout>;

  private userSubject = new BehaviorSubject<any>(null);
  user$ = this.userSubject.asObservable();

  // 🔥 Evita que se disparen dos llamadas a /auth/refresh en paralelo desde
  // esta misma pestaña (el refresh token es de un solo uso: la segunda
  // llamada siempre recibiría 401 porque el backend ya rotó el token).
  private refrescando = false;

  // 🔥 Evita mostrar el aviso de "sesión expirada" más de una vez si varias
  // peticiones fallan casi al mismo tiempo.
  private cerrandoSesionPorExpiracion = false;

  // 🔥 Sincroniza el token entre pestañas del mismo navegador. Si el usuario
  // duplica la pestaña (o abre la app en otra pestaña con la misma sesión),
  // sessionStorage se clona pero cada pestaña queda con su copia
  // independiente: si ambas intentan refrescar el mismo refresh token
  // (de un solo uso), la segunda recibe 401 y la sesión se cae sin motivo
  // real. Con este canal, apenas una pestaña refresca el token, todas las
  // demás lo reciben y actualizan su sessionStorage antes de intentar usar
  // el token viejo.
  private canal = typeof BroadcastChannel !== 'undefined'
    ? new BroadcastChannel('svua-auth')
    : null;

  constructor() {

    window.addEventListener('beforeunload', () => {
      this.stopRefreshTimer();
    });

    this.canal?.addEventListener('message', (event: MessageEvent<MensajeAuthBroadcast>) => {
      const data = event.data;

      if (data.type === 'tokens') {
        sessionStorage.setItem('token', data.accessToken);
        sessionStorage.setItem('refreshToken', data.refreshToken);
        this.setUserFromToken();
        this.startRefreshTimer();
      } else if (data.type === 'logout') {
        this.clearSession();
        this.router.navigateByUrl('/login');
      }
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

  getTokenJti(): string | null {
    const token = sessionStorage.getItem('token');
    if (!token) return null;

    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.jti;
  }

  getDemo(): boolean | null {
    const token = sessionStorage.getItem('token');
    if (!token) return null;

    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.demo;
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
      this.sesionExpirada('Tu sesión no es válida. Inicia sesión nuevamente.');
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

    // Ya hay un refresh en curso en esta pestaña: no dispares una segunda
    // llamada con el mismo refresh token (el backend lo rota en cada uso,
    // así que la segunda llamada siempre recibiría 401).
    if (this.refrescando) return;

    const refreshToken = sessionStorage.getItem('refreshToken');

    if (!refreshToken) {
      this.sesionExpirada();
      return;
    }

    this.refrescando = true;

    this.http.post<any>(`${this.apiUrl}/auth/refresh`, {
      refreshToken
    }).subscribe({
      next: (res) => {
        this.refrescando = false;

        sessionStorage.setItem('token', res.accessToken);
        sessionStorage.setItem('refreshToken', res.refreshToken);
        this.init();

        // 🔁 avisar a otras pestañas para que actualicen su token antes de
        // que intenten usar el que se acaba de invalidar
        this.canal?.postMessage({
          type: 'tokens',
          accessToken: res.accessToken,
          refreshToken: res.refreshToken
        } satisfies MensajeAuthBroadcast);

        // 🔁 reiniciar timer
        this.startRefreshTimer();
      },
      error: () => {
        this.refrescando = false;
        this.sesionExpirada();
      }
    });
  }

  validateToken(token: string) {
    return this.http.get(`${this.apiUrl}/auth/validate-token?token=${token}`);
  }

  resetPassword(token: string, password: string) {
    return this.http.post(`${this.apiUrl}/auth/reset-password`, { token, password });
  }

  /**
   * Cierre de sesión explícito (botón "Cerrar sesión"). Avisa al backend;
   * el llamador decide qué hacer con la respuesta, pero en cualquier caso
   * (éxito o error de red) debe terminar llamando a `finalizarSesionLocal()`
   * para dejar la sesión limpia en esta pestaña y en las demás.
   */
  logout() {
    this.stopRefreshTimer();

    return this.http.post(`${this.apiUrl}/auth/logout`, null);
  }

  /**
   * Limpia la sesión de esta pestaña y avisa a las demás pestañas del mismo
   * navegador para que hagan lo mismo (evita que una quede "viva" usando
   * datos de una sesión que ya se cerró en otro lado).
   */
  finalizarSesionLocal() {
    this.clearSession();
    this.canal?.postMessage({ type: 'logout' } satisfies MensajeAuthBroadcast);
  }

  /**
   * Sesión inválida o vencida (falló el refresh, o cualquier otra llamada
   * devolvió 401 con el token actual). Limpia todo, avisa al usuario con un
   * mensaje claro (en vez del salto silencioso a /login de antes) y avisa a
   * las demás pestañas para que hagan lo mismo.
   */
  sesionExpirada(mensaje = 'Tu sesión terminó. Inicia sesión nuevamente.') {

    if (this.cerrandoSesionPorExpiracion) return;
    this.cerrandoSesionPorExpiracion = true;

    this.finalizarSesionLocal();

    Swal.fire({
      icon: 'info',
      title: 'Sesión expirada',
      text: mensaje,
      confirmButtonText: 'Ir a iniciar sesión'
    }).then(() => {
      this.cerrandoSesionPorExpiracion = false;
      this.router.navigateByUrl('/login');
    });
  }

  clearSession() {

    this.stopRefreshTimer();

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

  isTecnico(): boolean {
    return this.getUserRole() === 'TECNICO';
  }
}
