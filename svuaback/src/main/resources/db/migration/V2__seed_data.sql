INSERT INTO empresa (
    empresa_id,
    activa,
    direccion,
    email_contacto,
    fecha_creacion,
    fecha_fin_plan,
    max_activos,
    max_usuarios,
    nombre,
    rut,
    telefono,
    tipo_plan
)
VALUES (
    1,
    true,
    'Av. Casa Matriz 1234',
    'contacto@casamatriz.cl',
    NOW(),
    '2036-05-06',
    999,
    999,
    'Casa Matriz SPA',
    '99.999.999-9',
    '+56912345678',
    'ENTERPRISE'
);

INSERT INTO empresa (
    empresa_id,
    activa,
    direccion,
    email_contacto,
    fecha_actualizacion,
    fecha_creacion,
    fecha_fin_plan,
    fecha_inicio_plan,
    max_activos,
    max_usuarios,
    nombre,
    rut,
    telefono,
    tipo_plan
)
VALUES (
    2,
    true,
    'Av. Presidente Eduardo Frei Montalva 9315, Quilicura',
    'mvega@fhalimentos.cl',
    NOW(),
    NOW(),
    '2026-05-21',
    '2026-05-06',
    20,
    2,
    'FH Alimentos SPA',
    '76.487.452-8',
    '+56933919318',
    'FREE'
);

INSERT INTO usuario (
    id_usuario,
    activo,
    email,
    intentos_fallidos,
    nombre,
    password,
    rol,
    empresa_id
)
VALUES (
    1,
    true,
    'dmedinac@gmail.com',
    0,
    'Admin Sistema',
    '$2a$10$17S4rbARxF6h48a9O/nfPOWYctDmfB1NiM4HOY/q5RLc0PGrwhVva',
    'SUPER_ADMIN',
    1
);

INSERT INTO usuario (
    id_usuario,
    activo,
    email,
    nombre,
    password,
    rol,
    empresa_id
)
VALUES (
    2,
    true,
    'contacto@svua.cl',
    'Mauricio Vega',
    '$2a$10$ST6UM3qrY8g3U5QuhAVZV.Wlfmfut6qB0e/.R.A5mebeVz2fhDrFq',
    'ADMIN_EMPRESA',
    2
);

SELECT setval('empresa_empresa_id_seq', 2, true);
SELECT setval('usuario_id_usuario_seq', 2, true);
