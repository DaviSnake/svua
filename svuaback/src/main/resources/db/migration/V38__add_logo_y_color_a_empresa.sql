-- Personalizacion por empresa: logo propio (reemplaza el logo generico
-- del sidebar) y color de acento (aplicado en puntos clave del sidebar:
-- barra lateral del item activo y su resaltado). Ver
-- EmpresaServiceImpl.subirLogo/obtenerLogo y sidebar.component.ts.
ALTER TABLE empresa
    ADD COLUMN logo_ruta_archivo VARCHAR(500),
    ADD COLUMN color_primario VARCHAR(7);
