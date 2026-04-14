type Plan = "FREE" | "BASIC" | "PRO";
export interface Empresa {
  id?: number;
  nombre: string;
  rut: string;
  emailContacto: string;
  telefono: string;
  direccion: string;
  tipoPlan: Plan | null;
}