CREATE TABLE sesion_usuario (

    id BIGSERIAL PRIMARY KEY,

    empresa_id BIGINT NOT NULL,
    id_usuario BIGINT NOT NULL,

    fecha_login TIMESTAMP,
    ultima_actividad TIMESTAMP,
    fecha_logout TIMESTAMP,

    pagina_actual VARCHAR(255),

    ip VARCHAR(100),
    navegador VARCHAR(255),
    sistema_operativo VARCHAR(255),

    activa BOOLEAN DEFAULT TRUE,

    cantidad_requests INTEGER DEFAULT 0,

    ultima_accion VARCHAR(500),

    version_app VARCHAR(100),

    dispositivo VARCHAR(100),

    token_jti VARCHAR(255),

    CONSTRAINT fk_sesion_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario),

    CONSTRAINT fk_sesion_empresa
        FOREIGN KEY (empresa_id)
        REFERENCES empresa(empresa_id)
);

CREATE INDEX idx_sesion_usuario
    ON sesion_usuario(id_usuario);

CREATE INDEX idx_sesion_empresa
    ON sesion_usuario(empresa_id);

CREATE INDEX idx_sesion_token
    ON sesion_usuario(token_jti);

CREATE INDEX idx_sesion_activa
    ON sesion_usuario(activa);

CREATE INDEX idx_sesion_ultima_actividad
    ON sesion_usuario(ultima_actividad);