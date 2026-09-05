CREATE TABLE contextos_academicos_estudiante (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    estudiante_id UUID NOT NULL REFERENCES estudiantes(id),
    carrera_id UUID NOT NULL,
    periodo_id UUID NOT NULL,
    nivel INTEGER NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_contexto_estudiante_nivel CHECK (nivel BETWEEN 1 AND 10),
    CONSTRAINT uq_contexto_estudiante_periodo UNIQUE (estudiante_id, periodo_id)
);

CREATE INDEX ix_contexto_estudiante_actual
    ON contextos_academicos_estudiante (estudiante_id, activo, creado_en DESC);

