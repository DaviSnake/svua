-- =====================================================================
-- Precarga de "Puntos de Control" para Empresa demo Spa (empresa_id = 2,
-- ver V2__seed_data.sql) -- modulo "Control de Turno"
-- (cl.aracridav.svua.controlturno). Los 26 puntos salen de la planilla
-- real que esta pantalla reemplaza
-- (Copia_de_SISTEMA_DE_CONTROL_DE_MANTENCION_21082026.xlsx, hoja
-- "HOJA DE CONTROL"): cada serie individual de los 11 graficos
-- combinados del Excel corresponde a un punto propio aca.
--
-- Los rangos aceptables (valor_min/valor_max) quedan sin definir a
-- proposito: el Excel original no trae umbrales explicitos, y sin
-- rango cada punto solo se grafica como linea de evolucion (sin la
-- dona de "dentro/fuera de rango" -- ver
-- LecturaControlServiceImpl.dashboard). Se pueden completar despues
-- desde la pantalla "Control de Turno", sin tocar la base de datos.
--
-- ⚠️ Las 3 filas de "Velocidad" (espirales, freidora 1, freidora 2)
-- quedan con unidad "RPM" como supuesto razonable: el Excel original
-- no trae una columna de unidad explicita para esas 3 -- confirmar y
-- editar si corresponde otra unidad real de planta.

INSERT INTO punto_control (nombre, unidad, activo, empresa_id) VALUES
    ('Proofer 1 - Temperatura Sección N°1', '°C', true, 2),
    ('Proofer 1 - Temperatura Sección N°2', '°C', true, 2),
    ('Proofer 1 - Temperatura Sección N°3', '°C', true, 2),
    ('Proofer 1 - Temperatura Sección N°4', '°C', true, 2),
    ('Proofer 1 - Humedad Sección N°1', '%', true, 2),
    ('Proofer 1 - Humedad Sección N°2', '%', true, 2),
    ('Proofer 1 - Humedad Sección N°3', '%', true, 2),
    ('Proofer 1 - Humedad Sección N°4', '%', true, 2),
    ('Proofer 2 - Temperatura Sección N°1', '°C', true, 2),
    ('Proofer 2 - Temperatura Sección N°2', '°C', true, 2),
    ('Proofer 2 - Temperatura Sección N°3', '°C', true, 2),
    ('Proofer 2 - Temperatura Sección N°4', '°C', true, 2),
    ('Proofer 2 - Humedad Sección N°1', '%', true, 2),
    ('Proofer 2 - Humedad Sección N°2', '%', true, 2),
    ('Proofer 2 - Humedad Sección N°3', '%', true, 2),
    ('Proofer 2 - Humedad Sección N°4', '%', true, 2),
    ('Cámara Variedades 1', '°C', true, 2),
    ('Cámara Variedades 2', '°C', true, 2),
    ('Cámara de Congelado', '°C', true, 2),
    ('Sala de Envasado', '°C', true, 2),
    ('Sala de Variedades', '°C', true, 2),
    ('Tiempo de Fermentación Proofer N°1', 'min', true, 2),
    ('Tiempo de Fermentación Proofer N°2', 'min', true, 2),
    ('Velocidad Espirales de Enfriado y Congelado', 'RPM', true, 2),
    ('Velocidad Freidora N°1', 'RPM', true, 2),
    ('Velocidad Freidora N°2', 'RPM', true, 2);
