-- Optimistic locking para Repuesto.stockActual: protege contra "lost
-- update" cuando dos operaciones concurrentes (ej. dos ordenes de
-- mantenimiento distintas usando el mismo repuesto) leen, restan y
-- guardan el stock casi al mismo tiempo, perdiendo una de las dos restas
-- sin que ninguna de las dos se entere.
ALTER TABLE repuesto
ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
