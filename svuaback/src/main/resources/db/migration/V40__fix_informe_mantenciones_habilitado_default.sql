-- V37 agrego esta columna en DEFAULT TRUE a proposito (para no ocultarle
-- la feature a empresas que ya la usaban sin restriccion). Pero para
-- empresas NUEVAS el criterio correcto es el mismo que
-- control_turno_habilitado/hoja_control_habilitado: opt-in, FALSE por
-- defecto (asi ya lo hace, de hecho, EmpresaServiceImpl.construirEmpresaBase
-- via Boolean.TRUE.equals(...), que manda FALSE explicito si el request
-- no trae el campo -- este ALTER solo alinea el DEFAULT de la columna
-- con eso, por si algun insert llega a saltarse ese codigo).
--
-- Los datos YA EXISTENTES no se tocan: las empresas que ya tenian TRUE
-- (via el backfill de V37) siguen viendolo habilitado.
ALTER TABLE empresa
    ALTER COLUMN informe_mantenciones_habilitado SET DEFAULT FALSE;
