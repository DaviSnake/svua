-- =====================================================================
-- fecha_creacion centralizada (BaseEntity)
-- =====================================================================
-- Agrega la columna "fecha_creacion" a todas las tablas del schema que
-- tengan una columna "empresa_id" (el mismo criterio usado en V27 para
-- RLS: son exactamente las tablas mapeadas por entidades que extienden
-- BaseEntity). El valor lo completa la aplicacion sola desde ahora
-- (@CreatedDate en BaseEntity, ver EnableJpaAuditing), sin tocar cada
-- servicio.
--
-- "activo" y "notificacion" ya tenian su propia columna "fecha_creacion"
-- (manejada antes a mano / con @CreationTimestamp en su propia entidad,
-- antes de centralizarla en BaseEntity) -- por eso el ADD COLUMN usa
-- IF NOT EXISTS: para esas dos tablas no hace nada y sus datos
-- historicos quedan intactos.
--
-- DEFAULT now() es solo para las filas ya existentes en las tablas que
-- todavia no tenian esta columna (no se conoce su fecha real de
-- creacion, asi que quedan con la fecha en que corre esta migracion).
-- Las filas nuevas reciben su fecha real desde la aplicacion.
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
        EXECUTE format(
            'ALTER TABLE %I ADD COLUMN IF NOT EXISTS fecha_creacion timestamp NOT NULL DEFAULT now()',
            tabla
        );
    END LOOP;
END $$;
