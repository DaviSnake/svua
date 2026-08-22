-- =====================================================================
-- Row Level Security (RLS) por empresa
-- =====================================================================
-- Hoy el aislamiento entre empresas es solo a nivel de aplicacion (el
-- @Filter de Hibernate "empresaFilter", ver EmpresaFilter /
-- EmpresaFilterAspect, mas los WHERE empresa_id = ? de cada repositorio).
-- Esta migracion agrega una segunda capa, a nivel de motor de base de
-- datos: aunque una query se olvide filtrar por empresa, Postgres
-- mismo no devuelve ni permite escribir filas de otra empresa.
--
-- Se aplica automaticamente a TODAS las tablas del schema que tengan
-- una columna "empresa_id" (BASE TABLE, no vistas) al momento de
-- correr esta migracion. Si en el futuro se agrega una tabla nueva con
-- empresa_id, hay que agregar una migracion nueva que repita este
-- mismo patron para esa tabla puntual (ENABLE + FORCE + CREATE POLICY).
--
-- La policy usa dos parametros de sesion que la aplicacion setea via
-- RlsContextService (ver EmpresaFilter, que lo llama en cada request
-- autenticado, y los schedulers que corren fuera de un request HTTP):
--   app.current_empresa_id -> id de la empresa del usuario actual
--   app.bypass_rls         -> 'on' para SUPER_ADMIN y para jobs
--                              internos de sistema que deben operar
--                              sobre mas de una empresa (mismo criterio
--                              que ya usa EmpresaFilter para saltarse
--                              el filtro de Hibernate en SUPER_ADMIN)
--
-- IMPORTANTE - accion manual en el servidor de produccion:
-- esto NO tiene efecto alguno si la conexion de la aplicacion usa un
-- rol de Postgres SUPERUSER: los superusers jamas quedan sujetos a
-- RLS, ni siquiera con FORCE ROW LEVEL SECURITY (es una excepcion
-- incondicional de Postgres, no un descuido de esta migracion). Hay
-- que confirmar que el POSTGRES_USER configurado en el .env de
-- produccion sea un rol normal, sin el atributo SUPERUSER ni
-- BYPASSRLS (el .env.example del proyecto ya lo modela asi: "svua",
-- no "postgres"). Verificar con:
--   SELECT rolname, rolsuper, rolbypassrls FROM pg_roles
--   WHERE rolname = '<POSTGRES_USER de produccion>';
-- Debe dar rolsuper=false y rolbypassrls=false. Si el rol de
-- produccion es "postgres" (superuser), hay que crear un rol nuevo sin
-- esos atributos, otorgarle los permisos de siempre sobre el schema
-- (GRANT ALL ON ALL TABLES/SEQUENCES IN SCHEMA public), y apuntar el
-- .env de produccion a ese rol antes de que esta migracion tenga algun
-- efecto real.
DO $$
DECLARE
    tabla text;
BEGIN
    FOR tabla IN
        SELECT DISTINCT c.table_name
        FROM information_schema.columns c
        JOIN information_schema.tables t
          ON t.table_schema = c.table_schema
         AND t.table_name = c.table_name
         AND t.table_type = 'BASE TABLE'
        WHERE c.table_schema = 'public'
          AND c.column_name = 'empresa_id'
    LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', tabla);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', tabla);

        EXECUTE format('DROP POLICY IF EXISTS empresa_isolation ON %I', tabla);

        EXECUTE format(
            'CREATE POLICY empresa_isolation ON %I
                USING (
                    current_setting(''app.bypass_rls'', true) = ''on''
                    OR empresa_id = NULLIF(current_setting(''app.current_empresa_id'', true), '''')::bigint
                )
                WITH CHECK (
                    current_setting(''app.bypass_rls'', true) = ''on''
                    OR empresa_id = NULLIF(current_setting(''app.current_empresa_id'', true), '''')::bigint
                )',
            tabla
        );
    END LOOP;
END $$;
