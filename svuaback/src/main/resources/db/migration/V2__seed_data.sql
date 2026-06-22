INSERT INTO empresa (
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
    tipo_plan,
    demo
)
VALUES (
    true,
    'COLO COLO 222 OFICINA 1008, COMUNA DE CONCEPCIÓN',
    'contacto@nexovectoria.cl',
    NOW(),
    '2036-05-06',
    999,
    999,
    'Nexo Vectoria SPA',
    '99.999.999-9',
    '+56912345678',
    'ENTERPRISE',
    false
);

INSERT INTO empresa (
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
    tipo_plan,
    demo
)
VALUES (
    true,
    'Demo 123, Santiago',
    'demo@empresademo.cl',
    NOW(),
    NOW(),
    '2036-05-21',
    '2026-05-06',
    999,
    999,
    'Empresa demo Spa',
    '11111111-1',
    '+56912345678',
    'ENTERPRISE',
    true
);

INSERT INTO usuario (
    activo,
    email,
    intentos_fallidos,
    nombre,
    password,
    rol,
    empresa_id
)
VALUES (
    true,
    'david.medina@nexovectoria.cl',
    0,
    'Admin Sistema',
    '$2a$10$17S4rbARxF6h48a9O/nfPOWYctDmfB1NiM4HOY/q5RLc0PGrwhVva',
    'SUPER_ADMIN',
    1
);

INSERT INTO usuario (
    activo,
    email,
    nombre,
    password,
    rol,
    empresa_id
)
VALUES (
    true,
    'mauricio.vega@nexovectoria.cl',
    'Mauricio Vega',
    '$2a$10$Tp3kTe3Qz8wCSltvFyP8guLgR5xV2qa9/PZA0okAF7tmTiJ6sXigu',
    'ADMIN_EMPRESA',
    1
);

INSERT INTO usuario (
    activo,
    email,
    nombre,
    password,
    rol,
    empresa_id
)
VALUES (
    true,
    'demo@empresademo.cl',
    'Demo',
    '$2a$10$Tp3kTe3Qz8wCSltvFyP8guLgR5xV2qa9/PZA0okAF7tmTiJ6sXigu',
    'ADMIN_EMPRESA',
    2
);

INSERT INTO ubicacion (
    activo, 
    descripcion, 
    direccion, 
    nombre, 
    empresa_id
)
VALUES (
    true, 
    'Ubicación 1 Demo', 
    'Dirección Demo 1, 123, Santiago', 
    'Ubicación 1 Demo', 
    2
);

INSERT INTO ubicacion (
    activo, 
    descripcion, 
    direccion, 
    nombre, 
    empresa_id
)
VALUES (
    true, 
    'Ubicación 2 Demo', 
    'Dirección Demo 2, 123, Santiago', 
    'Ubicación 2 Demo', 
    2
);

INSERT INTO ubicacion (
    activo, 
    descripcion, 
    direccion, 
    nombre, 
    empresa_id
)
VALUES (
    true, 
    'Ubicación 3 Demo', 
    'Dirección Demo 3, 123, Santiago', 
    'Ubicación 3 Demo', 
    2
);

INSERT INTO tipo_activo (
    activo, 
    descripcion, 
    nombre, 
    vida_util_referencial_meses, 
    empresa_id
)
VALUES (
    true, 
    'Tipo Activo 1 Demo', 
    'Tipo Activo 1 Demo', 
    12, 
    2
);

INSERT INTO tipo_activo (
    activo, 
    descripcion, 
    nombre, 
    vida_util_referencial_meses, 
    empresa_id
)
VALUES (
    true, 
    'Tipo Activo 2 Demo', 
    'Tipo Activo 2 Demo', 
    24, 
    2
);

INSERT INTO tipo_activo (
    activo, 
    descripcion, 
    nombre, 
    vida_util_referencial_meses, 
    empresa_id
)
VALUES (
    true, 
    'Tipo Activo 3 Demo', 
    'Tipo Activo 3 Demo', 
    36, 
    2
);

INSERT INTO proveedor (
    activo, 
    contacto, 
    email, 
    nombre, 
    rut, 
    telefono, 
    tipo_proveedor, 
    empresa_id
)
VALUES (
    true, 
    'Juan Pérez', 
    'contacto@proveedor1.cl', 
    'Proveedor Industrial SPA 1 Demo', 
    '76123456-1', 
    '56987654324', 
    'INTERNO', 
    2
);

INSERT INTO proveedor (
    activo, 
    contacto, 
    email, 
    nombre, 
    rut, 
    telefono, 
    tipo_proveedor, 
    empresa_id
)
VALUES (
    true, 
    'Luis Carcamo', 
    'contacto@proveedor2.cl', 
    'Proveedor Industrial SPA 2 Demo', 
    '76123456-2', 
    '56987654324', 
    'EXTERNO', 
    2
);
INSERT INTO proveedor (
    activo, 
    contacto, 
    email, 
    nombre, 
    rut, 
    telefono, 
    tipo_proveedor, 
    empresa_id
)
VALUES (
    true, 
    'Joseph Muñoz', 
    'contacto@proveedor3.cl', 
    'Proveedor Industrial SPA 3 Demo', 
    '76123456-3', 
    '56987654324', 
    'INTERNO', 
    2
);
INSERT INTO proveedor (
    activo, 
    contacto, 
    email, 
    nombre, 
    rut, 
    telefono, 
    tipo_proveedor, 
    empresa_id
)
VALUES (
    true, 
    'Alejandro Rivera', 
    'contacto@proveedor4.cl', 
    'Proveedor Industrial SPA 4 Demo', 
    '76123456-4', 
    '56987654324', 
    'EXTERNO', 
    2
);
INSERT INTO proveedor (
    activo, 
    contacto, 
    email, 
    nombre, 
    rut, 
    telefono, 
    tipo_proveedor, 
    empresa_id
)
VALUES (
    true, 
    'Lucia Marabolli', 
    'contacto@proveedor5.cl', 
    'Proveedor Industrial SPA 5 Demo', 
    '76123456-5', 
    '56987654324', 
    'INTERNO', 
    2
);
INSERT INTO proveedor (
    activo, 
    contacto, 
    email, 
    nombre, 
    rut, 
    telefono, 
    tipo_proveedor, 
    empresa_id
)
VALUES (
    true, 
    'Ana Ruiz', 
    'contacto@proveedor6.cl', 
    'Proveedor Industrial SPA 6 Demo', 
    '76123456-6', 
    '56987654324', 
    'EXTERNO', 
    2
);
INSERT INTO proveedor (
    activo, 
    contacto, 
    email, 
    nombre, 
    rut, 
    telefono, 
    tipo_proveedor, 
    empresa_id
)
VALUES (
    true, 
    'Pier Corleone', 
    'contacto@proveedor7.cl', 
    'Proveedor Industrial SPA 7 Demo', 
    '76123456-7', 
    '56987654324', 
    'INTERNO', 
    2
);
