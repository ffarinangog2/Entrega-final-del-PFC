-- Datos ficticios DEMO, deliberadamente fuera de Flyway productivo.
INSERT INTO facultades (id, codigo, nombre, descripcion) VALUES
('10000000-0000-0000-0000-000000000001', 'DEMO-FAC', 'Facultad DEMO', 'Datos ficticios para desarrollo')
ON CONFLICT (id) DO UPDATE SET nombre = excluded.nombre, descripcion = excluded.descripcion, activo = TRUE;

INSERT INTO carreras (id, facultad_id, codigo, nombre, descripcion) VALUES
('10000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001',
 'DEMO-CAR', 'Carrera DEMO', 'Datos ficticios para desarrollo')
ON CONFLICT (id) DO UPDATE SET facultad_id = excluded.facultad_id, nombre = excluded.nombre, activo = TRUE;

INSERT INTO periodos_lectivos (id, codigo, nombre, fecha_inicio, fecha_fin, estado) VALUES
('10000000-0000-0000-0000-000000000003', 'DEMO-2026', 'Período Lectivo DEMO 2026',
 '2026-01-01', '2026-12-31', 'ACTIVO')
ON CONFLICT (id) DO UPDATE SET nombre = excluded.nombre, fecha_inicio = excluded.fecha_inicio,
 fecha_fin = excluded.fecha_fin, estado = excluded.estado;

INSERT INTO campus (id, codigo, nombre, direccion) VALUES
('10000000-0000-0000-0000-000000000004', 'DEMO-CAMP', 'Campus DEMO', 'Dirección ficticia DEMO')
ON CONFLICT (id) DO UPDATE SET nombre = excluded.nombre, direccion = excluded.direccion, activo = TRUE;

INSERT INTO bloques (id, campus_id, codigo, nombre) VALUES
('10000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000004',
 'DEMO-BLOQ', 'Bloque DEMO')
ON CONFLICT (id) DO UPDATE SET campus_id = excluded.campus_id, nombre = excluded.nombre, activo = TRUE;

INSERT INTO pisos (id, bloque_id, numero, descripcion) VALUES
('10000000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000005', 1, 'Piso A DEMO'),
('10000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000005', 2, 'Piso B DEMO')
ON CONFLICT (id) DO UPDATE SET bloque_id = excluded.bloque_id, numero = excluded.numero,
 descripcion = excluded.descripcion, activo = TRUE;

INSERT INTO laboratorios (id, piso_id, codigo, nombre, capacidad, descripcion, estado) VALUES
('10000000-0000-0000-0000-000000000008', '10000000-0000-0000-0000-000000000006',
 'DEMO-LAB-A', 'Laboratorio A DEMO', 30, 'Laboratorio ficticio del piso A', 'DISPONIBLE'),
('10000000-0000-0000-0000-000000000009', '10000000-0000-0000-0000-000000000007',
 'DEMO-LAB-B', 'Laboratorio B DEMO', 25, 'Laboratorio ficticio del piso B', 'DISPONIBLE')
ON CONFLICT (id) DO UPDATE SET piso_id = excluded.piso_id, nombre = excluded.nombre,
 capacidad = excluded.capacidad, descripcion = excluded.descripcion, estado = excluded.estado, activo = TRUE;

INSERT INTO materias (id, carrera_id, codigo, nombre, numero_horas) VALUES
('10000000-0000-0000-0000-00000000000a', '10000000-0000-0000-0000-000000000002',
 'DEMO-MAT-A', 'Materia A DEMO', 64),
('10000000-0000-0000-0000-00000000000b', '10000000-0000-0000-0000-000000000002',
 'DEMO-MAT-B', 'Materia B DEMO', 48)
ON CONFLICT (id) DO UPDATE SET carrera_id = excluded.carrera_id, nombre = excluded.nombre,
 numero_horas = excluded.numero_horas, activo = TRUE;

INSERT INTO horarios_academicos
(id, materia_id, periodo_lectivo_id, laboratorio_id, docente_id, dia_semana, hora_inicio, hora_fin, paralelo) VALUES
('10000000-0000-0000-0000-00000000000c', '10000000-0000-0000-0000-00000000000a',
 '10000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000008',
 '30000000-0000-0000-0000-000000000001', 'LUNES', '08:00', '10:00', 'D1'),
('10000000-0000-0000-0000-00000000000d', '10000000-0000-0000-0000-00000000000b',
 '10000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000009',
 '30000000-0000-0000-0000-000000000001', 'MIERCOLES', '10:00', '12:00', 'D1')
ON CONFLICT (id) DO UPDATE SET materia_id = excluded.materia_id,
 periodo_lectivo_id = excluded.periodo_lectivo_id, laboratorio_id = excluded.laboratorio_id,
 docente_id = excluded.docente_id, dia_semana = excluded.dia_semana,
 hora_inicio = excluded.hora_inicio, hora_fin = excluded.hora_fin, activo = TRUE;
