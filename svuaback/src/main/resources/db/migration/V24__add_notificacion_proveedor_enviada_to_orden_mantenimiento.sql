-- Evita que el job diario de MantencionScheduler reenvie el mismo aviso
-- de mantencion programada al proveedor si corre mas de una vez el mismo
-- dia (redeploy/reinicio del backend o disparo manual del trigger).
ALTER TABLE orden_mantenimiento
ADD COLUMN notificacion_proveedor_enviada BOOLEAN NOT NULL DEFAULT FALSE;
