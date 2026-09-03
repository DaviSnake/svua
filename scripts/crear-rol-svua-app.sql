-- =====================================================================
-- Para ambientes donde el superusuario "bootstrap" del cluster de
-- Postgres NO se llama "postgres" sino algo como "svua_user" (esto pasa
-- cuando el contenedor se levanto con POSTGRES_USER=svua_user desde el
-- inicio: ese rol queda como EL UNICO superuser del cluster, y Postgres
-- se niega a quitarle SUPERUSER -- "ALTER ROLE svua_user WITH
-- NOSUPERUSER" falla con "The bootstrap user must have the SUPERUSER
-- attribute").
--
-- En vez de tocar ese rol administrador, se crea uno NUEVO ("svua") sin
-- SUPERUSER ni BYPASSRLS -- los dos atributos que hacen que Postgres
-- ignore Row Level Security incondicionalmente (ver
-- V27__enable_row_level_security_por_empresa.sql, seccion
-- "IMPORTANTE") -- y se cambia el backend para que se conecte con ESE
-- rol nuevo en vez del administrador.
--
-- Como correrlo:
--   1) Reemplazar CAMBIAR_password_fuerte por una password real.
--   2) Ejecutar TODO este archivo conectado como "svua_user" (el
--      administrador de este cluster -- SI puede crear roles nuevos,
--      la restriccion de Postgres es solo para quitarSE su propio
--      SUPERUSER, no para administrar otros roles).
--   3) Actualizar el .env REAL de ese ambiente (el que lee el backend
--      via docker-compose en el servidor, no el de tu IDE):
--        POSTGRES_USER=svua
--        POSTGRES_PASSWORD=<la password real de arriba>
--      "svua_user" NO se toca -- sigue existiendo, sigue siendo
--      superuser, simplemente el backend deja de usarlo para las
--      conexiones normales.
--   4) Reiniciar el backend de ese ambiente.
--   5) Verificar:
--        SELECT rolname, rolsuper, rolbypassrls FROM pg_roles WHERE rolname = 'svua';
--      Debe dar rolsuper=false y rolbypassrls=false.
--   6) Probar de inmediato: login, refresh de sesion, reset de
--      contraseña, y una operacion normal de la app -- es la primera
--      vez que RLS se aplica de verdad en este ambiente.
-- =====================================================================

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'svua') THEN
        CREATE ROLE svua WITH
            LOGIN
            PASSWORD 'Vale2904'
            NOSUPERUSER
            NOCREATEDB
            NOCREATEROLE
            NOBYPASSRLS
            NOREPLICATION;
    END IF;
END $$;

ALTER ROLE svua WITH
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    NOBYPASSRLS
    NOREPLICATION;

-- Permisos sobre el schema. CREATE es necesario porque Flyway corre las
-- migraciones (CREATE TABLE, ALTER TABLE, etc.) con esta misma conexion
-- una vez que el .env se actualice.
GRANT USAGE, CREATE ON SCHEMA public TO svua;

-- Permisos sobre TODO lo que ya existe hoy (creado hasta ahora por
-- "svua_user", el administrador): sin esto, "svua" se conectaria pero
-- recibiria "permission denied" en cada tabla existente.
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO svua;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO svua;
GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public TO svua;

-- Permisos por defecto sobre lo que se cree A FUTURO. Cubre ambos
-- casos: que las migraciones nuevas sigan corriendo como "svua_user"
-- (no deberia pasar despues del paso 3, pero por si acaso), o que ya
-- corran como "svua" (entonces se crea sus propias tablas sin
-- problema, esto no hace nada extra pero tampoco molesta).
ALTER DEFAULT PRIVILEGES FOR ROLE svua_user IN SCHEMA public
    GRANT ALL ON TABLES TO svua;
ALTER DEFAULT PRIVILEGES FOR ROLE svua_user IN SCHEMA public
    GRANT ALL ON SEQUENCES TO svua;
ALTER DEFAULT PRIVILEGES FOR ROLE svua IN SCHEMA public
    GRANT ALL ON TABLES TO svua;
ALTER DEFAULT PRIVILEGES FOR ROLE svua IN SCHEMA public
    GRANT ALL ON SEQUENCES TO svua;

-- Verificacion final (deberia devolver f | f)
SELECT rolname, rolsuper, rolbypassrls
FROM pg_roles
WHERE rolname = 'svua';
