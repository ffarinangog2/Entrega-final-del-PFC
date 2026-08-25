-- Identidades y adscripción organizacional ficticias. No contiene RBAC.
INSERT INTO perfiles (id, identificacion, nombres, apellidos, email_institucional) VALUES
('20000000-0000-0000-0000-000000000001', 'DEMO-DOC-001', 'Docente', 'DEMO', 'docente.demo@example.invalid'),
('20000000-0000-0000-0000-000000000002', 'DEMO-ADM-001', 'Administrador Piso', 'DEMO', 'admin.piso.demo@example.invalid')
ON CONFLICT (id) DO UPDATE SET nombres = excluded.nombres, apellidos = excluded.apellidos,
 email_institucional = excluded.email_institucional, activo = TRUE;

INSERT INTO docentes (id, perfil_id, codigo_docente, departamento, tipo_contrato, dedicacion) VALUES
('30000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001',
 'DEMO-DOC-001', 'Departamento DEMO', 'DEMO', 'DEMO')
ON CONFLICT (id) DO UPDATE SET perfil_id = excluded.perfil_id, departamento = excluded.departamento, activo = TRUE;

INSERT INTO administradores (id, perfil_id, codigo_administrador, cargo, piso_id) VALUES
('30000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002',
 'DEMO-ADM-001', 'Administrador de Piso DEMO', '10000000-0000-0000-0000-000000000006')
ON CONFLICT (id) DO UPDATE SET perfil_id = excluded.perfil_id, cargo = excluded.cargo,
 piso_id = excluded.piso_id, activo = TRUE;
