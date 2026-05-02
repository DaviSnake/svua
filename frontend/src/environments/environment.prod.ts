import { env } from './env.runtime';

export const environment = {
  production: true,
  apiUrl: env.apiUrl || 'http://localhost:8080/api/v1/svua'
};