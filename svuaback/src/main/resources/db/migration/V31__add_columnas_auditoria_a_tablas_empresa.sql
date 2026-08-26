-- =====================================================================
-- Trazabilidad de auditoria: quien y cuando modifico cada fila
-- =====================================================================
-- Complementa V28 (fecha_creacion): agrega fecha_modificacion,
-- creado_por_id y modificado_por_id a TODAS las tablas que ya tienen
-- fecha_creacion (es decir, todas las que extienden BaseEntity). Se
-- llenan solos en cada insert/update via Spring Data JPA auditing
-- (@CreatedBy/@LastModifiedDate/@LastModifiedBy en BaseEntity, ver
-- UsuarioAuditorAware para como se resuelve el usuario actual).
--
-- Fuera de un request HTTP autenticado (schedulers, jobs internos de
-- sistema -- ver RlsContextService) no hay Authentication en el
-- SecurityContext, asi que creado_por_id/modificado_por_id quedan en
-- NULL para esas filas: se interpreta como "sistema", no como un dato
-- faltante por error.
--
-- Los registros existentes (previos a esta migracion) tambien quedan
-- con estas 3 columnas en NULL: no hay forma de reconstruir
-- retroactivamente quien los creo/modifico, ese dato historico
-- simplemente no se capturaba antes de hoy.
--
-- ⚠️ Esto NO es un log de auditoria completo (no guarda cada version
-- de cada campo, solo la ULTIMA modificacion). Un historial completo
-- de cambios (que campo cambio, de que valor a que valor, cuando) es
-- una funcionalidad aparte y mas pesada (ej. con Hibernate Envers).
--
-- Si en el futuro se agrega una tabla nueva que extienda BaseEntity,
-- hay que agregar una migracion nueva que repita este mismo patron
-- para esa tabla puntual (mismo aviso que ya existe en V27).
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
          AND c.column_name = 'fecha_creacion'
    LOOP
        EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS fecha_modificacion TIMESTAMP', tabla);
        EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS creado_por_id BIGINT', tabla);
        EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS modificado_por_id BIGINT', tabla);

        -- FK a usuario.id_usuario, ON DELETE SET NULL: si el usuario
        -- que creo/modifico un registro se elimina despues, el dato
        -- queda en NULL en vez de bloquear el DELETE o perder la fila.
        EXECUTE format('ALTER TABLE %I DROP CONSTRAINT IF EXISTS %I', tabla, tabla || '_creado_por_id_fkey');
        EXECUTE format(
            'ALTER TABLE %I ADD CONSTRAINT %I FOREIGN KEY (creado_por_id)
                REFERENCES usuario (id_usuario) ON DELETE SET NULL',
            tabla, tabla || '_creado_por_id_fkey'
        );

        EXECUTE format('ALTER TABLE %I DROP CONSTRAINT IF EXISTS %I', tabla, tabla || '_modificado_por_id_fkey');
        EXECUTE format(
            'ALTER TABLE %I ADD CONSTRAINT %I FOREIGN KEY (modificado_por_id)
                REFERENCES usuario (id_usuario) ON DELETE SET NULL',
            tabla, tabla || '_modificado_por_id_fkey'
        );
    END LOOP;
END $$;
