CREATE TABLE solicitudes_retiro_planificacion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    planificacion_id UUID NOT NULL REFERENCES planificaciones(id),
    solicitante_perfil_id UUID NOT NULL,
    motivo TEXT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    creada_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resuelta_en TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_solicitud_retiro_estado
        CHECK (estado IN ('PENDIENTE', 'APROBADA', 'RECHAZADA')),
    CONSTRAINT ck_solicitud_retiro_motivo CHECK (length(trim(motivo)) > 0)
);

CREATE UNIQUE INDEX uq_solicitud_retiro_planificacion_pendiente
    ON solicitudes_retiro_planificacion (planificacion_id)
    WHERE estado = 'PENDIENTE';

CREATE INDEX ix_solicitud_retiro_planificacion_historial
    ON solicitudes_retiro_planificacion (planificacion_id, creada_en DESC);

CREATE TABLE decisiones_retiro_planificacion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    solicitud_retiro_id UUID NOT NULL REFERENCES solicitudes_retiro_planificacion(id),
    piso_id UUID NOT NULL,
    revisada_por_perfil_id UUID,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    observacion TEXT,
    creada_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resuelta_en TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_decision_retiro_solicitud_piso UNIQUE (solicitud_retiro_id, piso_id),
    CONSTRAINT ck_decision_retiro_estado
        CHECK (estado IN ('PENDIENTE', 'APROBADA', 'RECHAZADA'))
);

CREATE INDEX ix_decision_retiro_piso_estado
    ON decisiones_retiro_planificacion (piso_id, estado);
