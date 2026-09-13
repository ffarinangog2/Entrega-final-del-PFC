CREATE TABLE planificaciones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    carrera_id UUID NOT NULL,
    periodo_id UUID NOT NULL,
    coordinador_perfil_id UUID NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'BORRADOR',
    creada_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizada_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    enviada_en TIMESTAMPTZ,
    aprobada_en TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_planificacion_carrera_ciclo UNIQUE (carrera_id, periodo_id),
    CONSTRAINT ck_planificacion_agregada_estado CHECK (
        estado IN ('BORRADOR', 'EN_REVISION', 'REQUIERE_CAMBIOS', 'APROBADA', 'FINALIZADA')
    )
);

ALTER TABLE planificaciones_semestre
    ADD COLUMN IF NOT EXISTS planificacion_id UUID,
    ADD COLUMN IF NOT EXISTS nivel SMALLINT;

ALTER TABLE planificaciones_semestre
    ADD CONSTRAINT IF NOT EXISTS fk_bloque_planificacion
        FOREIGN KEY (planificacion_id) REFERENCES planificaciones(id),
    ADD CONSTRAINT IF NOT EXISTS ck_bloque_nivel
        CHECK (nivel IS NULL OR nivel BETWEEN 1 AND 10);

CREATE INDEX IF NOT EXISTS ix_bloques_planificacion_nivel
    ON planificaciones_semestre (planificacion_id, nivel);

CREATE TABLE revisiones_planificacion_piso (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    planificacion_id UUID NOT NULL REFERENCES planificaciones(id),
    piso_id UUID NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    observacion TEXT,
    revisada_por_perfil_id UUID,
    creada_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizada_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_revision_planificacion_piso UNIQUE (planificacion_id, piso_id),
    CONSTRAINT ck_revision_planificacion_estado CHECK (
        estado IN ('PENDIENTE', 'APROBADA', 'RECHAZADA', 'PROPUESTA_CAMBIO')
    )
);

CREATE TABLE observaciones_revision_planificacion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    revision_id UUID NOT NULL REFERENCES revisiones_planificacion_piso(id),
    bloque_id UUID NOT NULL REFERENCES planificaciones_semestre(id),
    laboratorio_propuesto_id UUID,
    observacion TEXT NOT NULL,
    creada_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX ix_observaciones_revision
    ON observaciones_revision_planificacion (revision_id, bloque_id);

-- Los bloques legacy permanecen sin cabecera: no existe información fiable para
-- distinguir envíos históricos diferentes de una misma carrera y periodo.
