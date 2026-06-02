ALTER TABLE orden_mantenimiento
DROP CONSTRAINT chk_orden_estado;

ALTER TABLE orden_mantenimiento
ADD CONSTRAINT chk_orden_estado
CHECK (
    estado IN (
        'PENDIENTE',
        'PROGRAMADA',
        'EN_EJECUCION',
        'PRE_COMPLETADA',
        'COMPLETADA',
        'CANCELADA'
    )
);