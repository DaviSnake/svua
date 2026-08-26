-- Agrega a Empresa un flag booleano que permite habilitar, por empresa,
-- el modulo "Control de Turno" (catalogo de puntos de control + registro
-- de lecturas por turno + dashboard de graficos). Mismo patron que
-- codigo_qr_habilitado/codigo_ean13_habilitado (V23): antes el unico
-- criterio de acceso era el ROL del usuario (SUPER_ADMIN/ADMIN_EMPRESA/
-- JEFE_MANTENIMIENTO/TECNICO), sin distincion por empresa -- cualquier
-- empresa con un usuario de esos roles veia el menu, aunque no use el
-- modulo.
ALTER TABLE empresa
    ADD COLUMN control_turno_habilitado BOOLEAN NOT NULL DEFAULT FALSE;

-- Backfill: la empresa que ya tiene puntos de control cargados (ver
-- V30__seed_puntos_control_demo.sql, "Empresa demo Spa") mantiene acceso
-- para no perderlo de un dia para otro. Se puede desactivar despues
-- desde la pantalla Empresa si corresponde.
UPDATE empresa e
SET control_turno_habilitado = TRUE
WHERE EXISTS (
    SELECT 1 FROM punto_control pc WHERE pc.empresa_id = e.empresa_id
);
