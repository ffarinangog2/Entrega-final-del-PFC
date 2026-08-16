INSERT INTO perfiles (
    id,
    identificacion,
    nombres,
    apellidos,
    email_institucional,
    activo
)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    '0000000000',
    'Administrador',
    'del Sistema',
    'admin@scli.local',
    TRUE
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO administradores (
    perfil_id,
    codigo_administrador,
    cargo,
    activo
)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'ADM-0001',
    'Administrador del Sistema',
    TRUE
)
ON CONFLICT (perfil_id) DO NOTHING;