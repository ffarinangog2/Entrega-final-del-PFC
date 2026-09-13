ALTER TABLE periodos_lectivos
    ADD COLUMN IF NOT EXISTS ppa_codigo VARCHAR(40),
    ADD COLUMN IF NOT EXISTS ppa_nombre VARCHAR(120),
    ADD COLUMN IF NOT EXISTS ciclo_academico SMALLINT;

ALTER TABLE periodos_lectivos
    ADD CONSTRAINT IF NOT EXISTS ck_periodos_ciclo_academico
        CHECK (ciclo_academico IS NULL OR ciclo_academico BETWEEN 1 AND 2);

ALTER TABLE materias
    ADD COLUMN IF NOT EXISTS nivel SMALLINT;

ALTER TABLE materias
    ADD CONSTRAINT IF NOT EXISTS ck_materias_nivel
        CHECK (nivel IS NULL OR nivel BETWEEN 1 AND 10);

CREATE INDEX IF NOT EXISTS idx_materias_carrera_nivel
    ON materias (carrera_id, nivel);

INSERT INTO periodos_lectivos (
    id, codigo, nombre, fecha_inicio, fecha_fin, estado,
    ppa_codigo, ppa_nombre, ciclo_academico
) VALUES
    ('3F000000-0000-0000-0000-000000000001', 'PPA-2026-2027-C1',
     'Ciclo academico Mayo-Septiembre', '2026-05-01', '2026-09-18', 'PLANIFICADO',
     'REGULAR-2026-2027-PPA', 'REGULAR - 2026-2027 PPA', 1),
    ('3F000000-0000-0000-0000-000000000002', 'PPA-2026-2027-C2',
     'Ciclo academico Noviembre-Abril', '2026-11-01', '2027-04-30', 'PLANIFICADO',
     'REGULAR-2026-2027-PPA', 'REGULAR - 2026-2027 PPA', 2)
ON CONFLICT (codigo) DO UPDATE SET
    ppa_codigo = excluded.ppa_codigo,
    ppa_nombre = excluded.ppa_nombre,
    ciclo_academico = excluded.ciclo_academico;

UPDATE materias
SET nivel = CASE codigo
    WHEN 'SW-101' THEN 1
    WHEN 'SW-202' THEN 2
    WHEN 'SW-303' THEN 3
    WHEN 'TI-101' THEN 1
    WHEN 'TI-202' THEN 2
    WHEN 'TI-303' THEN 3
    ELSE nivel
END
WHERE codigo IN ('SW-101', 'SW-202', 'SW-303', 'TI-101', 'TI-202', 'TI-303')
  AND nivel IS NULL;
