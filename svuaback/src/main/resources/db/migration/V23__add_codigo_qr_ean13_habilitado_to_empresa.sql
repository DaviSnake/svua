-- Agrega a Empresa dos flags booleanos independientes que permiten
-- habilitar, por empresa, la generacion/uso de codigo QR y codigo EAN13
-- para sus activos (antes esta funcionalidad estaba disponible solo para
-- SUPER_ADMIN y para la empresa marcada como demo).
ALTER TABLE empresa
    ADD COLUMN codigo_qr_habilitado BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN codigo_ean13_habilitado BOOLEAN NOT NULL DEFAULT FALSE;

-- Backfill: la empresa que ya estaba marcada como demo mantenia acceso a
-- esta funcionalidad antes de este cambio (via Empresa.demo), asi que se
-- le activan ambos flags para no perder ese acceso de un dia para otro.
-- Se puede desactivar cualquiera de los dos despues desde la pantalla
-- Empresa si corresponde.
UPDATE empresa
SET codigo_qr_habilitado = TRUE,
    codigo_ean13_habilitado = TRUE
WHERE demo = TRUE;
