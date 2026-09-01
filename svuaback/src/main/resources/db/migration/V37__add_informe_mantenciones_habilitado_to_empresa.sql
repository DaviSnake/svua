-- Agrega a Empresa un flag booleano que permite habilitar/deshabilitar,
-- por empresa, el "Informe de Mantenciones" (ver
-- OrdenMantenimientoController.obtenerInformeMantenciones). Mismo
-- patron que control_turno_habilitado/hoja_control_habilitado (V33/V36).
--
-- A diferencia de esos dos flags (features nuevas, opt-in), el Informe
-- de Mantenciones ya estaba disponible sin restriccion por empresa para
-- SUPER_ADMIN/ADMIN_EMPRESA -- por eso el default es TRUE (Postgres
-- rellena las filas existentes con el default al agregar la columna),
-- para no ocultarselo de un dia para otro a nadie que ya lo usaba. Se
-- puede desactivar despues, por empresa, desde la pantalla Empresa.
ALTER TABLE empresa
    ADD COLUMN informe_mantenciones_habilitado BOOLEAN NOT NULL DEFAULT TRUE;
