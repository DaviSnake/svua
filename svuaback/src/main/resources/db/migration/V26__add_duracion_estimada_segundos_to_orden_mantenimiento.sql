-- 🔥 duracion_segundos queda reservada para el TIEMPO REAL de ejecucion
-- (se sobrescribe al pre-completar/cancelar la orden, ver calcularDuracion
-- en OrdenMantenimientoServiceImpl). Antes de este cambio, esa misma
-- columna tambien se usaba para sembrar la duracion ESTIMADA al crear la
-- orden, perdiendose una vez que la orden se ejecutaba de verdad — lo que
-- hacia inconsistente cualquier calculo que necesitara el estimado
-- despues de ese punto (por ejemplo, reprogramar preservando la duracion
-- originalmente planificada). Esta columna guarda esa duracion estimada
-- de forma permanente, sin tocarse nunca despues de creada la orden.
ALTER TABLE orden_mantenimiento
ADD COLUMN duracion_estimada_segundos BIGINT;

-- Backfill: para toda orden existente, fecha_programada/fecha_termino
-- siguen representando la ventana ORIGINALMENTE planificada (nunca se
-- tocan al ejecutar/pre-completar/completar la orden), asi que su
-- diferencia es la duracion estimada correcta sin importar el estado
-- actual de la orden.
UPDATE orden_mantenimiento
SET duracion_estimada_segundos = EXTRACT(EPOCH FROM (fecha_termino - fecha_programada))
WHERE fecha_programada IS NOT NULL AND fecha_termino IS NOT NULL;
