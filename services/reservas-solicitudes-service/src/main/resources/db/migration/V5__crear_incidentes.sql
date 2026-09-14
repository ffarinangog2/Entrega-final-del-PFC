CREATE TABLE incidentes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reportante_id UUID NOT NULL,
    laboratorio_equipo VARCHAR(200) NOT NULL,
    descripcion VARCHAR(2000) NOT NULL,
    prioridad VARCHAR(10) NOT NULL,
    fecha DATE NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'REPORTADO',
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_incidentes_laboratorio_equipo CHECK (btrim(laboratorio_equipo) <> ''),
    CONSTRAINT ck_incidentes_descripcion CHECK (btrim(descripcion) <> ''),
    CONSTRAINT ck_incidentes_prioridad CHECK (prioridad IN ('BAJA', 'MEDIA', 'ALTA')),
    CONSTRAINT ck_incidentes_estado CHECK (estado IN ('REPORTADO', 'EN_REVISION', 'RESUELTO'))
);

CREATE INDEX ix_incidentes_reportante_creado ON incidentes (reportante_id, creado_en DESC);
CREATE INDEX ix_incidentes_estado_prioridad ON incidentes (estado, prioridad);
