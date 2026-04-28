export interface PerfilUsuario {
  id: number;
  nombre: string;
  email: string;
  rol: string;
  activo: boolean;

  empresaNombre: string;
  empresaRut: string;
  plan: string;
  fechaFinPlan: string;
}
