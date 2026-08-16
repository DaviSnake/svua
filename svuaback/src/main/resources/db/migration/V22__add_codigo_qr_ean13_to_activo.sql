-- Agregar columnas
ALTER TABLE activo
ADD COLUMN codigo_qr VARCHAR(150);

ALTER TABLE activo
ADD COLUMN codigo_ean13 VARCHAR(13);

-- Backfill de activos ya existentes: el QR es simplemente el codigo interno.
UPDATE activo
SET codigo_qr = codigo_interno
WHERE codigo_qr IS NULL;

-- Backfill de EAN13 para activos existentes: se deriva un numero de 12
-- digitos desde un hash del codigo interno y se le agrega el digito
-- verificador estandar de EAN13. Este hash (hashtext de Postgres) no es
-- identico al hashCode que usa el backend (ActivoCodigoGenerador), pero
-- eso no importa aqui: es solo para poblar los activos ya existentes con
-- un valor valido y estable; el codigo se regenera con el algoritmo del
-- backend si el codigo interno del activo cambia mas adelante.
WITH base AS (
    SELECT id_activo,
           lpad((abs(hashtext(codigo_interno)::bigint) % 1000000000000)::text, 12, '0') AS base12
    FROM activo
    WHERE codigo_ean13 IS NULL
),
calculo AS (
    SELECT id_activo,
           base12,
           (10 - (
               (
                   (substring(base12,1,1)::int * 1) + (substring(base12,2,1)::int * 3) +
                   (substring(base12,3,1)::int * 1) + (substring(base12,4,1)::int * 3) +
                   (substring(base12,5,1)::int * 1) + (substring(base12,6,1)::int * 3) +
                   (substring(base12,7,1)::int * 1) + (substring(base12,8,1)::int * 3) +
                   (substring(base12,9,1)::int * 1) + (substring(base12,10,1)::int * 3) +
                   (substring(base12,11,1)::int * 1) + (substring(base12,12,1)::int * 3)
               ) % 10
           )) % 10 AS digito_verificador
    FROM base
)
UPDATE activo a
SET codigo_ean13 = c.base12 || c.digito_verificador
FROM calculo c
WHERE a.id_activo = c.id_activo;
