CREATE TABLE notificacion (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(255),
    mensaje VARCHAR(1000),
    leida BOOLEAN NOT NULL DEFAULT FALSE,
    tipo_notificacion VARCHAR(100) NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    empresa_id BIGINT NOT NULL,

    CONSTRAINT fk_notificacion_empresa
        FOREIGN KEY (empresa_id)
        REFERENCES empresa(empresa_id)
);


CREATE INDEX idx_notificacion_empresa
ON notificacion(empresa_id);


CREATE INDEX idx_notificacion_fecha
ON notificacion(fecha_creacion);

CREATE INDEX idx_notificacion_leida
ON notificacion(leida);