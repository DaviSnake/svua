import { env } from './env.runtime';

export const environment = {
  production: false,
  apiUrl: env.apiUrl || 'http://localhost:8080/api/v1/svua',
  wsUrl: env.wsUrl || 'ws://localhost:8080/ws',
  // 🔥 Minutos de inactividad antes de cerrar sesión automáticamente
  // (ver InactivityService). Configurable en assets/env.js sin
  // necesidad de recompilar el frontend.
  tiempoInactividadMinutos: Number(env.tiempoInactividadMinutos) || 5
};