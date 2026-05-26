ALTER TABLE orden_mantenimiento
ADD COLUMN id_proveedor BIGINT;

ALTER TABLE orden_mantenimiento
ADD COLUMN horas_estimadas_proveedor NUMERIC(15,2);

ALTER TABLE orden_mantenimiento
ADD COLUMN horas_reales_proveedor NUMERIC(15,2);

ALTER TABLE orden_mantenimiento
ADD COLUMN valor_hora_proveedor NUMERIC(15,2);

ALTER TABLE orden_mantenimiento
ADD COLUMN costo_mano_obra_proveedor NUMERIC(15,2);

ALTER TABLE orden_mantenimiento
ADD CONSTRAINT fk_orden_proveedor
FOREIGN KEY (id_proveedor)
REFERENCES proveedor(id_proveedor);