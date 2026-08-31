INSERT INTO permisos (codigo, nombre, descripcion)
VALUES
    ('INCIDENTE_CREAR', 'Reportar incidentes', 'Permite reportar incidentes operativos'),
    ('INCIDENTE_LEER', 'Consultar incidentes propios', 'Permite consultar los incidentes reportados por el usuario'),
    ('INCIDENTE_GESTIONAR', 'Gestionar incidentes', 'Permite consultar y cambiar el estado de incidentes'),
    ('NOTIFICACION_DISPOSITIVO', 'Registrar dispositivo', 'Permite asociar un dispositivo de notificaciones al usuario autenticado')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo IN ('INCIDENTE_CREAR', 'INCIDENTE_LEER', 'NOTIFICACION_DISPOSITIVO')
WHERE r.codigo IN ('ADMINISTRADOR', 'ADMINISTRADOR_PISO', 'COORDINADOR', 'DOCENTE', 'ESTUDIANTE')
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo = 'INCIDENTE_GESTIONAR'
WHERE r.codigo IN ('ADMINISTRADOR', 'ADMINISTRADOR_PISO')
ON CONFLICT (rol_id, permiso_id) DO NOTHING;
