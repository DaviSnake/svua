ALTER TABLE notificacion
ADD COLUMN referencia_id BIGINT;

ALTER TABLE notificacion
ADD COLUMN tipo_referencia VARCHAR(50);

CREATE INDEX idx_notificacion_referencia
ON notificacion(tipo_referencia, referencia_id);