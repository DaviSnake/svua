import { env } from './env.runtime';

export const environment = {
  production: true,
  apiUrl: env.apiUrl || 'https://api.svua.cl/api/v1/svua',
  wsUrl: env.wsUrl || 'wss://api.svua.cl/ws',
  // 🔥 Minutos de inactividad antes de cerrar sesión automáticamente
  // (ver InactivityService). Configurable en assets/env.js sin
  // necesidad de recompilar el frontend.
  tiempoInactividadMinutos: Number(env.tiempoInactividadMinutos) || 5
};