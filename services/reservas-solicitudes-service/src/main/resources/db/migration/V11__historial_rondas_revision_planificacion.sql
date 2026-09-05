ALTER TABLE revisiones_planificacion_piso
    ADD COLUMN IF NOT EXISTS ronda INT NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS vigente BOOL NOT NULL DEFAULT true;

DROP INDEX IF EXISTS uq_revision_planificacion_piso CASCADE;

CREATE UNIQUE INDEX IF NOT EXISTS uq_revision_planificacion_piso_vigente
    ON revisiones_planificacion_piso (planificacion_id, piso_id)
    WHERE vigente = true;

CREATE INDEX IF NOT EXISTS ix_revision_planificacion_historial
    ON revisiones_planificacion_piso (planificacion_id, ronda, creada_en);
