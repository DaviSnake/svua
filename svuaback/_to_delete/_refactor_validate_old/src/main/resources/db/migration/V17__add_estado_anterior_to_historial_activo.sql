-- Agregar columna
ALTER TABLE historial_estado_activo
ADD COLUMN estado_anterior VARCHAR(50);

-- Inicializar registros existentes
UPDATE historial_estado_activo
SET estado_anterior = estado
WHERE estado_anterior IS NULL;

-- Hacer obligatoria la columna
ALTER TABLE historial_estado_activo
ALTER COLUMN estado_anterior SET NOT NULL;