-- Agrega a Empresa un flag booleano que permite habilitar, por empresa,
-- el boton "Importar Excel (HOJA DE CONTROL)" dentro de Control de
-- Turno (ver HojaControlImportServiceImpl). Mismo patron que
-- codigo_qr_habilitado/control_turno_habilitado (V23/V33): el parser es
-- especifico al layout de una planilla real de una empresa puntual, no
-- generico -- no tiene sentido mostrarlo a empresas que no usan esa
-- planilla.
ALTER TABLE empresa
    ADD COLUMN hoja_control_habilitado BOOLEAN NOT NULL DEFAULT FALSE;

-- Backfill: la empresa que ya tiene puntos de control cargados (ver
-- V30__seed_puntos_control_demo.sql, "Empresa demo Spa") es la que este
-- importador reemplaza, asi que parte habilitada. Se puede desactivar
-- despues desde la pantalla Empresa si corresponde.
UPDATE empresa e
SET hoja_control_habilitado = TRUE
WHERE EXISTS (
    SELECT 1 FROM punto_control pc WHERE pc.empresa_id = e.empresa_id
);
