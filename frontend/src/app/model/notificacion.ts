export interface Notificacion {
  id: number;
  titulo: string;
  mensaje: string;
  leida: boolean;
  tipoNotificacion: string;
  referenciaId?: number;
  tipoReferencia?: string;
  fechaCreacion: string;
}