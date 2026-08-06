-- Agregar columna
ALTER TABLE activo
ADD COLUMN fecha_creacion TIMESTAMP;

-- Inicializar registros existentes
UPDATE activo
SET fecha_creacion = NOW()
WHERE fecha_creacion IS NULL;

-- Hacer obligatoria la columna
ALTER TABLE activo
ALTER COLUMN fecha_creacion SET NOT NULL;