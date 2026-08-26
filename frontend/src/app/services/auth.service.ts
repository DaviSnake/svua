import { HttpClient } from '@angular/common/http';
import { inject, Inject, Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { LoginRequest } from '../auth/models/login-request';
import { LoginResponse } from '../auth/models/login-response';
import { jwtDecode } from 'jwt-decode';
import { BehaviorSubject, Observable, catchError, finalize, shareReplay, tap, throwError } from 'rxjs';
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

  // 🔥 Single-flight: si ya hay un refresh en curso (disparado por el timer
  // proactivo o por un 401 de cualquier endpoint), todos los que lo pidan
  // mientras tanto reciben el MISMO resultado en vez de disparar llamadas
  // duplicadas (el refresh token es de un solo uso: una segunda llamada en
  // paralelo con el mismo token siempre recibiría 401 del backend).
  private refreshInProgress$: Observable<any> | null = null;

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

  // 🔥 Evita mandar el aviso de cierre más de una vez si 'beforeunload' y
  // 'pagehide' se disparan los dos para el mismo cierre de pestaña.
  private avisoCierreEnviado = false;

  constructor() {

    window.addEventListener('beforeunload', () => {
      this.stopRefreshTimer();
      this.avisarCierrePestana();
    });

    // 🔥 En navegadores móviles (Safari en particular) 'beforeunload' no es
    // confiable; 'pagehide' es el evento recomendado para detectar el
    // cierre real de la pestaña/ventana en cualquier dispositivo. Si
    // event.persisted es true, la página solo entra a la bfcache (podría
    // "revivir" con el botón atrás) y no se considera un cierre real.
    window.addEventListener('pagehide', (event: PageTransitionEvent) => {
      if (!event.persisted) {
        this.avisarCierrePestana();
      }
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

  // 🔒 Empresa.codigoQrHabilitado / codigoEan13Habilitado (viajan en el JWT
  // igual que demo): controlan si el escaneo de activos y el QR/EAN13 del
  // modal "Ver" estan disponibles para la empresa del usuario logueado.
  getCodigoQrHabilitado(): boolean | null {
    const token = sessionStorage.getItem('token');
    if (!token) return null;

    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.codigoQrHabilitado;
  }

  getCodigoEan13Habilitado(): boolean | null {
    const token = sessionStorage.getItem('token');
    if (!token) return null;

    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.codigoEan13Habilitado;
  }

  // 🔒 Empresa.controlTurnoHabilitado (viaja en el JWT igual que demo):
  // controla si el modulo Control de Turno esta disponible para la
  // empresa del usuario logueado (ver sidebar.component.ts y
  // control-turno-acceso.guard.ts).
  getControlTurnoHabilitado(): boolean | null {
    const token = sessionStorage.getItem('token');
    if (!token) return null;

    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.controlTurnoHabilitado;
  }

  // 🐛 FIX: init() se llama en CADA carga de la app (AppComponent.ngOnInit,
  // incluido un simple refresh/F5 de la pagina), pero antes solo restauraba
  // el usuario desde el token y NUNCA volvia a armar el timer de refresco
  // proactivo (startRefreshTimer solo se llamaba en el login y despues de
  // un refresh exitoso). Resultado: apenas el usuario hacia F5, el access
  // token quedaba "huerfano" -> vencia en silencio sin nadie renovandolo a
  // tiempo, y la sesion terminaba dependiendo por completo del refresh
  // REACTIVO (el que dispara el interceptor ante un 401), aumentando el
  // riesgo de terminar usando un refresh token viejo/ya rotado y recibir
  // 401 en /auth/refresh (token de un solo uso que el backend ya no
  // encuentra).
  init() {
    this.setUserFromToken();
    this.startRefreshTimer();
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

  /**
   * Refresco "silencioso" del access token. Se usa desde dos lugares:
   *  1) El timer proactivo (`startRefreshTimer`), 2 minutos antes de que
   *     venza el token.
   *  2) El interceptor `sessionExpiredInterceptor`, cuando CUALQUIER
   *     petición devuelve 401: en vez de cerrar sesión de inmediato, se
   *     intenta renovar el token y reintentar esa misma petición. Solo si
   *     este refresh también falla (el refresh token venció o ya no es
   *     válido) es que la sesión terminó de verdad.
   *
   * `refreshInProgress$` asegura que, si llegan varias peticiones con 401
   * casi al mismo tiempo, todas esperen el MISMO refresh en vez de disparar
   * llamadas duplicadas (el refresh token es de un solo uso).
   */
  refrescarToken(): Observable<any> {

    if (this.refreshInProgress$) {
      return this.refreshInProgress$;
    }

    const refreshToken = sessionStorage.getItem('refreshToken');

    if (!refreshToken) {
      this.sesionExpirada();
      return throwError(() => new Error('No hay refresh token'));
    }

    this.refreshInProgress$ = this.http.post<any>(`${this.apiUrl}/auth/refresh`, {
      refreshToken
    }).pipe(
      tap((res) => {
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

        // 🔁 reiniciar timer del refresco proactivo
        this.startRefreshTimer();
      }),
      catchError((err) => {
        // El refresh token ya no sirve: ahí sí terminó la sesión de verdad.
        this.sesionExpirada();
        return throwError(() => err);
      }),
      finalize(() => {
        this.refreshInProgress$ = null;
      }),
      shareReplay(1)
    );

    return this.refreshInProgress$;
  }

  /**
   * Refresco proactivo "fire and forget" (disparado por el timer). No
   * necesita reintentar ninguna petición, solo dejar el token renovado
   * antes de que venza.
   */
  getRefreshToken() {
    this.refrescarToken().subscribe({ error: () => { /* ya manejado en refrescarToken() */ } });
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
   * Al cerrar la pestaña/ventana (en cualquier dispositivo), avisa al
   * backend para marcar ESTA sesión como desconectada de inmediato en
   * "Sesiones Activas", en vez de esperar el job de limpieza por
   * inactividad (2 horas). Usa fetch con keepalive: true en lugar de un
   * HttpClient/XHR normal porque es el único mecanismo que:
   *   1) el navegador garantiza seguir enviando aunque la página ya se
   *      esté destruyendo (un HttpClient normal se cancela), y
   *   2) permite mandar el header Authorization (navigator.sendBeacon no
   *      admite headers personalizados, así que no sirve acá).
   *
   * IMPORTANTE: esto SOLO marca la sesión como cerrada en el backend; no
   * borra el token local ni redirige a /login, para no romper un simple
   * refresh (F5) de la página. Si el usuario solo refrescó, la próxima
   * petición normal (registro de actividad) vuelve a marcar la sesión
   * como activa automáticamente.
   */
  private avisarCierrePestana(): void {

    if (this.avisoCierreEnviado) {
      return;
    }

    const token = sessionStorage.getItem('token');
    const tokenJti = this.getTokenJti();

    if (!token || !tokenJti) {
      return;
    }

    this.avisoCierreEnviado = true;

    fetch(`${this.apiUrl}/sesiones/logout/${tokenJti}`, {
      method: 'POST',
      keepalive: true,
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: '{}'
    }).catch(() => {
      // Ignorado: la pestaña ya se está cerrando, no hay forma de
      // reintentar ni de mostrar feedback al usuario.
    });
  }

  /**
   * Detecta si el servidor tiene desplegada una versión del frontend más
   * nueva que la que está corriendo en esta pestaña, y si es así fuerza
   * una navegación real (no del router de Angular) hacia `destino` en vez
   * de dejar que el usuario siga usando el bundle viejo (el típico caso
   * de "no veo los cambios hasta que hago Ctrl+Shift+F5"). Se llama al
   * iniciar sesión.
   *
   * Funciona en cualquier navegador porque no depende de ninguna API de
   * "limpiar cache" (esa API no existe para el cache HTTP normal): en vez
   * de eso, compara version.json (que se regenera en cada build, ver
   * scripts/generar-version.js) pedido con `cache: 'no-store'` — así el
   * navegador SIEMPRE va a la red a buscarlo, sin importar qué tan
   * agresivo sea su cache — contra la última versión que esta pestaña
   * había visto.
   *
   * Si difieren, en vez de un simple window.location.reload() (que
   * recargaría la pantalla de login en la que el usuario sigue parado)
   * se asigna window.location.href = destino: eso fuerza una navegación
   * de página completa —no una ruta del SPA— que trae un index.html
   * fresco (nginx ya lo sirve con no-store, ver nginx.conf) con los
   * bundles JS/CSS de la versión nueva, y de paso deja al usuario en su
   * pantalla de inicio (el token ya quedó guardado antes de llamar esto).
   *
   * Devuelve true si se disparó la navegación forzada (el llamador no
   * debe navegar por su cuenta, la página ya se está yendo).
   */
  async verificarVersionYRecargarSiCorresponde(destino: string): Promise<boolean> {

    try {

      const resp = await fetch(`/assets/version.json?t=${Date.now()}`, {
        cache: 'no-store'
      });

      if (!resp.ok) {
        return false;
      }

      const data = await resp.json();
      const versionServidor = data?.version;

      if (!versionServidor) {
        return false;
      }

      const versionConocida = sessionStorage.getItem('svua_version_conocida');

      // 🔥 Primera vez que esta pestaña ve la app: solo guarda la
      // referencia, nada que comparar todavía.
      if (!versionConocida) {
        sessionStorage.setItem('svua_version_conocida', versionServidor);
        return false;
      }

      if (versionConocida !== versionServidor) {
        sessionStorage.setItem('svua_version_conocida', versionServidor);
        window.location.href = destino;
        return true;
      }

      return false;

    } catch {
      // Sin red, version.json no existe (ej. entorno de desarrollo con
      // "ng serve"), etc.: no bloquea el login por esto.
      return false;
    }
  }

  /**
   * Sesión inválida o vencida DE VERDAD: falló el refresh del token (o no
   * había refresh token disponible). Limpia todo, avisa al usuario con un
   * mensaje claro y avisa a las demás pestañas para que hagan lo mismo.
   *
   * IMPORTANTE: esto ya NO se llama por cualquier 401 de cualquier
   * endpoint — eso ahora primero intenta renovar el token y reintentar la
   * petición (ver `refrescarToken()` y `sessionExpiredInterceptor`). Solo
   * se llega acá cuando ese refresh también falla.
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
