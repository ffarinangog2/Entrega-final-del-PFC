INSERT INTO adscripciones_institucionales (
    id, perfil_id, tipo_ambito, ambito_id, activo
)
SELECT
    '27000000-0000-0000-0000-000000000001',
    '22000000-0000-0000-0000-000000000007',
    'CARRERA',
    '37000000-0000-0000-0000-000000000001',
    TRUE
WHERE EXISTS (
    SELECT 1 FROM perfiles
    WHERE id = '22000000-0000-0000-0000-000000000007'
)
ON CONFLICT (perfil_id, tipo_ambito, ambito_id) DO NOTHING;

INSERT INTO adscripciones_institucionales (
    id, perfil_id, tipo_ambito, ambito_id, activo
)
SELECT
    '27000000-0000-0000-0000-000000000002',
    '22000000-0000-0000-0000-000000000008',
    'CARRERA',
    '37000000-0000-0000-0000-000000000002',
    TRUE
WHERE EXISTS (
    SELECT 1 FROM perfiles
    WHERE id = '22000000-0000-0000-0000-000000000008'
)
ON CONFLICT (perfil_id, tipo_ambito, ambito_id) DO NOTHING;
