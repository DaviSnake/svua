import { env } from './env.runtime';

export const environment = {
  production: true,
  apiUrl: env.apiUrl || 'https://api.svua.cl/api/v1/svua',
  wsUrl: env.wsUrl || 'wss://api.svua.cl/ws'
};