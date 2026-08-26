-- =====================================================================
-- Historial de cambios (Hibernate Envers) para PuntoControl
-- =====================================================================
-- Complementa V32: en ese momento PuntoControl/LecturaControl quedaron
-- afuera porque el modulo Control de Turno vivia en la rama "graficos",
-- todavia no fusionada. Ya fusionada a "desarrollo", PuntoControl entra
-- al alcance acordado (es catalogo de configuracion, igual que
-- TipoActivo/Ubicacion -- ver PuntoControl.java, ahora @Audited).
--
-- LecturaControl sigue quedando AFUERA a proposito: son lecturas
-- horarias que se insertan una vez y no se modifican (misma logica que
-- LecturaTemperatura en V32), asi que no aporta valor auditarlas.
--
-- Mismo patron de V32: FK de relacion sin constraint (para que el
-- historial sobreviva aunque se borre el punto de control referenciado
-- en el futuro), RLS aplicado al final.

CREATE TABLE punto_control_aud (
    rev BIGINT NOT NULL REFERENCES revision_info (rev),
    revtype SMALLINT,
    id_punto_control BIGINT NOT NULL,

    nombre VARCHAR(150),
    unidad VARCHAR(20),
    valor_min NUMERIC(10,2),
    valor_max NUMERIC(10,2),
    activo BOOLEAN,

    empresa_id BIGINT,
    fecha_creacion TIMESTAMP,
    creado_por_id BIGINT,
    fecha_modificacion TIMESTAMP,
    modificado_por_id BIGINT,

    PRIMARY KEY (rev, id_punto_control)
);

-- =====================================================================
-- Row Level Security para punto_control_aud
-- =====================================================================
-- Mismo patron que V27/V32: se repite acotado a la tabla nueva de esta
-- migracion (revision_info y las 12 _aud de V32 ya quedaron con su
-- politica aplicada, este bloque las vuelve a tocar sin efecto real
-- ya que DROP POLICY IF EXISTS + CREATE POLICY es idempotente).
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
          AND c.table_name = 'punto_control_aud'
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
