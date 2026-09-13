-- Los placeholders son reemplazados en memoria por seed-demo.ps1 con hashes BCrypt.
INSERT INTO usuarios_auth (id, perfil_id, username, email, password_hash) VALUES
('40000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001',
 'demo.docente', 'docente.demo@example.invalid', '__DOCENTE_PASSWORD_HASH__'),
('40000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002',
 'demo.admin.piso', 'admin.piso.demo@example.invalid', '__ADMIN_PISO_PASSWORD_HASH__')
ON CONFLICT (id) DO UPDATE SET perfil_id = excluded.perfil_id, username = excluded.username,
 email = excluded.email, password_hash = excluded.password_hash, activo = TRUE, cuenta_bloqueada = FALSE;

INSERT INTO usuarios_roles (usuario_id, rol_id)
SELECT '40000000-0000-0000-0000-000000000001', id FROM roles WHERE codigo = 'DOCENTE'
ON CONFLICT (usuario_id, rol_id) DO NOTHING;

INSERT INTO usuarios_roles (usuario_id, rol_id)
SELECT '40000000-0000-0000-0000-000000000002', id FROM roles WHERE codigo = 'ADMINISTRADOR_PISO'
ON CONFLICT (usuario_id, rol_id) DO NOTHING;
