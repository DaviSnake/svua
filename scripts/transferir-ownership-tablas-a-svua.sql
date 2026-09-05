-- =====================================================================
-- Transfiere la PROPIEDAD (ownership) de las tablas y secuencias de
-- "public" al rol "svua", que la app ya usa para conectarse (ver
-- crear-rol-svua-app.sql). Ese script otorga permisos DML
-- (SELECT/INSERT/UPDATE/DELETE) sobre lo existente, pero eso NO
-- incluye ser el DUEÑO de esas tablas -- y hacer ALTER TABLE (agregar
-- una columna, cambiar un default, etc., como hace cualquier migracion
-- de Flyway) requiere ser el dueño, no solo tener esos permisos.
--
-- Sin esto, cualquier migracion que altere una tabla creada ANTES del
-- cambio de rol falla con:
--   ERROR: must be owner of table <tabla>
--
-- Por que NO usar simplemente "REASSIGN OWNED BY svua_user TO svua":
-- si "svua_user" es el superusuario BOOTSTRAP del cluster (el
-- POSTGRES_USER con el que se levanto el contenedor por primera vez --
-- ver el caso que cubre crear-rol-svua-app.sql), tambien es dueño de
-- objetos internos del sistema (ej. el lenguaje "plpgsql"), y
-- REASSIGN OWNED intenta moverlo TODO de una sola vez -- falla
-- completo con:
--   ERROR: cannot reassign ownership of objects owned by role
--   svua_user because they are required by the database system
--
-- Este script evita ese problema por completo: solo toca tablas y
-- secuencias de "public" (lo que Flyway necesita), nunca la base de
-- datos, el schema, ni nada a nivel de sistema.
--
-- Como correrlo (una sola vez por ambiente/base de datos, DESPUES de
-- crear-rol-svua-app.sql y de actualizar el .env real a POSTGRES_USER=svua):
--   docker compose exec -T postgres psql -U svua_user -d svua -f - < scripts/transferir-ownership-tablas-a-svua.sql
-- (conectado como "svua_user", el dueño actual -- no como "svua", que
-- todavia no es dueño de nada en este punto).
--
-- Si el nombre del rol administrador/bootstrap de ese ambiente no es
-- "svua_user" (ej. es "postgres", ver crear-rol-svua.sql), no hace
-- falta este script: ese caso ya no tiene el problema, porque las
-- tablas fueron creadas por "postgres" y jamas se le pidio a NADIE que
-- dejara de serlo -- "postgres" sigue pudiendo hacer ALTER TABLE
-- siempre, sea o no superuser.
-- =====================================================================

DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN SELECT tablename FROM pg_tables WHERE schemaname = 'public' LOOP
        EXECUTE format('ALTER TABLE public.%I OWNER TO svua', r.tablename);
    END LOOP;

    FOR r IN SELECT sequencename FROM pg_sequences WHERE schemaname = 'public' LOOP
        EXECUTE format('ALTER SEQUENCE public.%I OWNER TO svua', r.sequencename);
    END LOOP;
END $$;

-- Verificacion final: no deberia quedar ninguna tabla de "public" con
-- otro dueño que no sea "svua".
SELECT tablename, tableowner
FROM pg_tables
WHERE schemaname = 'public' AND tableowner <> 'svua';
