type Plan = "FREE" | "BASIC" | "PRO";
export interface Empresa {
  id?: number;
  nombre: string;
  rut: string;
  emailContacto: string;
  telefono: string;
  direccion: string;
  tipoPlan: Plan | null;
  activa?: boolean;
  demo?: boolean;
  codigoQrHabilitado?: boolean;
  codigoEan13Habilitado?: boolean;
  controlTurnoHabilitado?: boolean;
  hojaControlHabilitado?: boolean;
}