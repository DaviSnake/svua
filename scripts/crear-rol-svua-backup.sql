-- =====================================================================
-- Crea un rol de SOLO LECTURA dedicado exclusivamente al backup
-- automatico de Postgres (ver docker-compose.yml, servicio
-- "postgres-backup", y scripts/backup-postgres.sh).
--
-- Por que un rol aparte en vez de reusar "svua" (el que usa el
-- backend): "svua" se creo a proposito SIN el atributo BYPASSRLS (ver
-- crear-rol-svua.sql) -- es la ultima linea de defensa contra que un
-- bug en el codigo de aislamiento por empresa (RlsContextService) deje
-- ver datos de otra empresa, aunque falle el filtro de la aplicacion.
--
-- pg_dump, en cambio, necesita ver TODAS las filas de TODAS las
-- empresas para que el backup sirva de algo, y por defecto corre con
-- row_security=off -- con FORCE ROW SECURITY (V27) eso obliga a que el
-- rol conectado tenga BYPASSRLS, sin excepcion (ningun truco de sesion
-- lo evita, es una restriccion del motor). Si le dieramos BYPASSRLS al
-- rol "svua" para arreglar el backup, se lo estariamos dando tambien
-- al backend en produccion -- se pierde esa ultima linea de defensa
-- para TODO el trafico normal de la app, no solo para el backup.
--
-- Este rol nuevo ("svua_backup") es de solo lectura (SELECT, nunca
-- INSERT/UPDATE/DELETE) y BYPASSRLS, y solo lo usa el contenedor
-- "postgres-backup" -- el backend nunca se conecta con el.
--
-- Como correrlo (una sola vez por ambiente/base de datos):
--   1) Reemplazar CAMBIAR_password_fuerte mas abajo por una password
--      real (la misma que despues va en BACKUP_POSTGRES_PASSWORD del
--      .env de ese ambiente).
--   2) Ejecutar TODO este archivo conectado como "postgres" (o el rol
--      superuser que se use hoy), contra la base de datos correcta.
--   3) Actualizar el .env de ese ambiente:
--        BACKUP_POSTGRES_USER=svua_backup
--        BACKUP_POSTGRES_PASSWORD=<la password real que se puso arriba>
--   4) Recrear el contenedor postgres-backup (docker compose up -d
--      postgres-backup) para que tome las variables nuevas.
--   5) Verificar que quedo bien:
--        SELECT rolname, rolsuper, rolbypassrls FROM pg_roles WHERE rolname = 'svua_backup';
--      Debe dar rolsuper=false y rolbypassrls=true.
--   6) Probar un backup manual (docker exec <backup_container> sh
--      /backup-postgres.sh) y confirmar que YA NO aparece el error
--      "query would be affected by row-level security policy".
-- =====================================================================

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'svua_backup') THEN
        CREATE ROLE svua_backup WITH
            LOGIN
            PASSWORD 'CAMBIAR_password_fuerte'
            NOSUPERUSER
            NOCREATEDB
            NOCREATEROLE
            BYPASSRLS
            NOREPLICATION;
    END IF;
END $$;

-- Por si el rol ya existia con otros atributos: lo deja explicitamente
-- en el estado correcto.
ALTER ROLE svua_backup WITH
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    BYPASSRLS
    NOREPLICATION;

GRANT CONNECT ON DATABASE svua TO svua_backup;
GRANT USAGE ON SCHEMA public TO svua_backup;

-- Solo lectura sobre TODO lo que ya existe hoy. Nunca INSERT/UPDATE/
-- DELETE: este rol jamas deberia poder modificar datos.
GRANT SELECT ON ALL TABLES IN SCHEMA public TO svua_backup;
GRANT SELECT ON ALL SEQUENCES IN SCHEMA public TO svua_backup;

-- Permisos por defecto sobre tablas que se creen A FUTURO (migraciones
-- de Flyway nuevas, que corren como "svua" en este ambiente -- ver
-- crear-rol-svua-app.sql). Sin esto, cada tabla nueva quedaria
-- invisible para el backup hasta correr este GRANT de nuevo a mano.
ALTER DEFAULT PRIVILEGES FOR ROLE svua IN SCHEMA public
    GRANT SELECT ON TABLES TO svua_backup;
ALTER DEFAULT PRIVILEGES FOR ROLE svua IN SCHEMA public
    GRANT SELECT ON SEQUENCES TO svua_backup;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public
    GRANT SELECT ON TABLES TO svua_backup;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public
    GRANT SELECT ON SEQUENCES TO svua_backup;

-- Verificacion final (deberia dar f | t)
SELECT rolname, rolsuper, rolbypassrls
FROM pg_roles
WHERE rolname = 'svua_backup';
