export interface SesionUsuarioResponse {
  id: number;
  usuarioId: number;
  usuario: string;
  empresa: string;

  fechaLogin: string;
  ultimaActividad: string;
  fechaLogout?: string | null;

  paginaActual: string | null;
  ultimaAccion: string | null;

  ip: string | null;
  navegador: string | null;
  sistemaOperativo: string | null;
  dispositivo: string | null;

  cantidadRequests: number;
  activa: boolean;
}