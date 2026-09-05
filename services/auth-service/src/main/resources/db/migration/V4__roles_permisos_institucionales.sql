INSERT INTO roles (codigo, nombre, descripcion)
VALUES
    (
        'COORDINADOR',
        'Coordinador',
        'Gestiona la planificacion academica semestral'
    ),
    (
        'ADMINISTRADOR_PISO',
        'Administrador de Piso',
        'Gestiona la operacion de los laboratorios de un piso'
    )
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO permisos (codigo, nombre, descripcion)
VALUES
    (
        'ACADEMICO_LEER',
        'Consultar informacion academica',
        'Permite consultar facultades, carreras, materias, periodos, horarios y estructura academica'
    ),
    (
        'PLANIFICACION_GESTIONAR',
        'Gestionar planificacion academica',
        'Permite crear y modificar la planificacion academica semestral'
    )
ON CONFLICT (codigo) DO NOTHING;

-- El administrador tecnico/general conserva acceso completo, incluidos
-- los permisos institucionales agregados por esta migracion.
INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permisos p
WHERE r.codigo = 'ADMINISTRADOR'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo = 'ACADEMICO_LEER'
WHERE r.codigo IN ('DOCENTE', 'ESTUDIANTE')
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo IN (
    'ACADEMICO_LEER',
    'SOLICITUD_LEER',
    'SOLICITUD_APROBAR',
    'SOLICITUD_RECHAZAR',
    'RESERVA_LEER',
    'RESERVA_CANCELAR',
    'LABORATORIO_LEER',
    'LABORATORIO_GESTIONAR',
    'EQUIPO_LEER',
    'AGENDA_GESTIONAR'
)
WHERE r.codigo = 'ADMINISTRADOR_PISO'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo IN (
    'ACADEMICO_LEER',
    'PLANIFICACION_GESTIONAR',
    'LABORATORIO_LEER'
)
WHERE r.codigo = 'COORDINADOR'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;
