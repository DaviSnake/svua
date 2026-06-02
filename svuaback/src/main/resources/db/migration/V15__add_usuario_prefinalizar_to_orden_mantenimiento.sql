ALTER TABLE orden_mantenimiento
ADD COLUMN id_usuario_pre_finalizacion BIGINT;

ALTER TABLE orden_mantenimiento
ADD CONSTRAINT fk_orden_usuario_pre_finalizacion
    FOREIGN KEY (id_usuario_pre_finalizacion)
    REFERENCES usuario(id_usuario)