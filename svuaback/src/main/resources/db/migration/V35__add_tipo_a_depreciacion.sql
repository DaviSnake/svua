-- Depreciación acelerada tributaria (SII, Art. 31 N°5 LIR): se calcula
-- en paralelo a la depreciación normal, sobre 1/3 de la vida útil, con
-- la misma fórmula de línea recta. `tipo` distingue ambos cronogramas
-- dentro de las mismas tablas.

ALTER TABLE depreciacion
    ADD COLUMN tipo VARCHAR(20) NOT NULL DEFAULT 'NORMAL';

ALTER TABLE depreciacion
    ADD CONSTRAINT depreciacion_tipo_check
    CHECK (tipo IN ('NORMAL', 'ACELERADA'));

ALTER TABLE depreciacion
    ALTER COLUMN tipo DROP DEFAULT;

ALTER TABLE depreciacion_mensual
    ADD COLUMN tipo VARCHAR(20) NOT NULL DEFAULT 'NORMAL';

ALTER TABLE depreciacion_mensual
    ADD CONSTRAINT depreciacion_mensual_tipo_check
    CHECK (tipo IN ('NORMAL', 'ACELERADA'));

ALTER TABLE depreciacion_mensual
    ALTER COLUMN tipo DROP DEFAULT;
