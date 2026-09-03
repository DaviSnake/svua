-- =====================================================================
-- Crea el rol de conexion "svua" que debe usar la aplicacion (backend +
-- Flyway), SIN el atributo SUPERUSER ni BYPASSRLS -- son los dos unicos
-- que hacen que Postgres ignore Row Level Security incondicionalmente
-- (ver V27__enable_row_level_security_por_empresa.sql, seccion
-- "IMPORTANTE"). Sin este rol, RLS esta habilitado en el schema pero
-- nunca se aplica de verdad: cualquier conexion como "postgres"
-- (superuser) ve y escribe filas de TODAS las empresas sin filtrar,
-- sin ningun error que lo delate.
--
-- Como correrlo (una sola vez por ambiente/base de datos):
--   1) Reemplazar CAMBIAR_password_fuerte mas abajo por una password
--      real (la misma que despues va en POSTGRES_PASSWORD del .env de
--      ese ambiente).
--   2) Ejecutar TODO este archivo conectado como "postgres" (o el rol
--      superuser que se use hoy), contra la base de datos correcta.
--      Por ejemplo:
--        docker compose exec -T postgres psql -U postgres -d svua -f - < scripts/crear-rol-svua.sql
--      o pegando el contenido en pgAdmin/DBeaver conectado como postgres.
--   3) Actualizar el .env de ese ambiente:
--        POSTGRES_USER=svua
--        POSTGRES_PASSWORD=<la password real que se puso arriba>
--   4) Reiniciar el backend (docker compose restart backend, o el
--      equivalente local) para que tome la nueva conexion.
--   5) Verificar que quedo bien:
--        SELECT rolname, rolsuper, rolbypassrls FROM pg_roles WHERE rolname = 'svua';
--      Debe dar rolsuper=false y rolbypassrls=false.
-- =====================================================================

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'svua') THEN
        CREATE ROLE svua WITH
            LOGIN
            PASSWORD 'root123'
            NOSUPERUSER
            NOCREATEDB
            NOCREATEROLE
            NOBYPASSRLS
            NOREPLICATION;
    END IF;
END $$;

-- Por si el rol ya existia con otros atributos (ej. se creo antes sin
-- querer con SUPERUSER): lo deja explicitamente en el estado correcto.
ALTER ROLE svua WITH
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    NOBYPASSRLS
    NOREPLICATION;

-- Permisos sobre la base y el schema. CREATE en el schema es necesario
-- porque Flyway corre las migraciones (CREATE TABLE, ALTER TABLE, etc.)
-- con esta misma conexion.
GRANT CONNECT ON DATABASE svua TO svua;
GRANT USAGE, CREATE ON SCHEMA public TO svua;

-- Permisos sobre TODO lo que ya existe hoy en el schema (creado hasta
-- ahora por "postgres"): sin esto, svua podria conectarse pero
-- recibiria "permission denied" en cada tabla existente.
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO svua;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO svua;
GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public TO svua;

-- Permisos por defecto sobre lo que se cree A FUTURO. Se deja
-- registrado para ambos roles (postgres Y svua) como "creador", porque
-- despues del paso 3 las migraciones nuevas de Flyway van a correr
-- como "svua" (crea sus propias tablas, sin problema), pero esto cubre
-- igual el caso de que alguna migracion puntual se corra a mano como
-- "postgres".
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public
    GRANT ALL ON TABLES TO svua;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public
    GRANT ALL ON SEQUENCES TO svua;
ALTER DEFAULT PRIVILEGES FOR ROLE svua IN SCHEMA public
    GRANT ALL ON TABLES TO svua;
ALTER DEFAULT PRIVILEGES FOR ROLE svua IN SCHEMA public
    GRANT ALL ON SEQUENCES TO svua;
