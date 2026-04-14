export interface Usuario {
  id?: number;
  nombre: string;
  email: string;
  password?: string;
  empresaNombre: string;
  empresaId: number;
  activo: boolean;
  rol: 'SUPER_ADMIN' | 'ADMIN_EMPRESA' | 'JEFE_MANTENIMIENTO' | 'TECNICO' | 'BODEGUERO' | 'USUARIO';
}