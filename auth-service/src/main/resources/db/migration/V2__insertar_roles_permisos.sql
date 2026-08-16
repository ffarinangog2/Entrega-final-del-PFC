INSERT INTO roles (
    codigo,
    nombre,
    descripcion
)
VALUES
    (
        'ADMINISTRADOR',
        'Administrador',
        'Acceso completo al sistema'
    ),
    (
        'DOCENTE',
        'Docente',
        'Puede solicitar y consultar reservas'
    ),
    (
        'TECNICO',
        'Técnico',
        'Gestiona laboratorios, equipos y solicitudes'
    ),
    (
        'ESTUDIANTE',
        'Estudiante',
        'Puede consultar información autorizada'
    )
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO permisos (
    codigo,
    nombre,
    descripcion
)
VALUES
    (
        'USUARIO_LEER',
        'Consultar usuarios',
        'Permite consultar usuarios'
    ),
    (
        'USUARIO_CREAR',
        'Crear usuarios',
        'Permite registrar usuarios'
    ),
    (
        'USUARIO_EDITAR',
        'Editar usuarios',
        'Permite modificar usuarios'
    ),
    (
        'USUARIO_DESACTIVAR',
        'Desactivar usuarios',
        'Permite desactivar cuentas'
    ),
    (
        'SOLICITUD_CREAR',
        'Crear solicitudes',
        'Permite crear solicitudes de reserva'
    ),
    (
        'SOLICITUD_LEER',
        'Consultar solicitudes',
        'Permite consultar solicitudes'
    ),
    (
        'SOLICITUD_APROBAR',
        'Aprobar solicitudes',
        'Permite aprobar solicitudes'
    ),
    (
        'SOLICITUD_RECHAZAR',
        'Rechazar solicitudes',
        'Permite rechazar solicitudes'
    ),
    (
        'SOLICITUD_CANCELAR',
        'Cancelar solicitudes',
        'Permite cancelar solicitudes'
    ),
    (
        'RESERVA_LEER',
        'Consultar reservas',
        'Permite consultar reservas'
    ),
    (
        'RESERVA_CANCELAR',
        'Cancelar reservas',
        'Permite cancelar reservas'
    ),
    (
        'LABORATORIO_LEER',
        'Consultar laboratorios',
        'Permite consultar laboratorios'
    ),
    (
        'LABORATORIO_GESTIONAR',
        'Gestionar laboratorios',
        'Permite administrar laboratorios'
    ),
    (
        'EQUIPO_LEER',
        'Consultar equipos',
        'Permite consultar equipos'
    ),
    (
        'EQUIPO_GESTIONAR',
        'Gestionar equipos',
        'Permite administrar equipos'
    ),
    (
        'REPORTE_LEER',
        'Consultar reportes',
        'Permite consultar reportes'
    ),
    (
        'REPORTE_GENERAR',
        'Generar reportes',
        'Permite generar reportes'
    ),
    (
        'AGENDA_GESTIONAR',
        'Gestionar agenda',
        'Permite administrar la agenda de laboratorios'
    )
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO roles_permisos (
    rol_id,
    permiso_id
)
SELECT
    r.id,
    p.id
FROM roles r
CROSS JOIN permisos p
WHERE r.codigo = 'ADMINISTRADOR'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO roles_permisos (
    rol_id,
    permiso_id
)
SELECT
    r.id,
    p.id
FROM roles r
JOIN permisos p
    ON p.codigo IN (
        'SOLICITUD_CREAR',
        'SOLICITUD_LEER',
        'SOLICITUD_CANCELAR',
        'RESERVA_LEER',
        'LABORATORIO_LEER'
    )
WHERE r.codigo = 'DOCENTE'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO roles_permisos (
    rol_id,
    permiso_id
)
SELECT
    r.id,
    p.id
FROM roles r
JOIN permisos p
    ON p.codigo IN (
        'SOLICITUD_LEER',
        'SOLICITUD_APROBAR',
        'SOLICITUD_RECHAZAR',
        'RESERVA_LEER',
        'RESERVA_CANCELAR',
        'LABORATORIO_LEER',
        'LABORATORIO_GESTIONAR',
        'EQUIPO_LEER',
        'EQUIPO_GESTIONAR',
        'REPORTE_LEER',
        'AGENDA_GESTIONAR'
    )
WHERE r.codigo = 'TECNICO'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO roles_permisos (
    rol_id,
    permiso_id
)
SELECT
    r.id,
    p.id
FROM roles r
JOIN permisos p
    ON p.codigo IN (
        'RESERVA_LEER',
        'LABORATORIO_LEER'
    )
WHERE r.codigo = 'ESTUDIANTE'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;