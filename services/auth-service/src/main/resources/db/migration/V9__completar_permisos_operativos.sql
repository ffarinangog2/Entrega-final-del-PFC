INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo = 'EQUIPO_GESTIONAR'
WHERE r.codigo = 'ADMINISTRADOR_PISO'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo = 'RESERVA_CANCELAR'
WHERE r.codigo = 'DOCENTE'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;
