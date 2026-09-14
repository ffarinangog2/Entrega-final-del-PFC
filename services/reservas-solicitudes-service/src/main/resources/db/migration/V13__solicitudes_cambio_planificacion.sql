CREATE TABLE solicitudes_cambio_planificacion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    planificacion_id UUID NOT NULL REFERENCES planificaciones(id),
    bloque_id UUID NOT NULL REFERENCES planificaciones_semestre(id),
    tipo VARCHAR(30) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    motivo TEXT NOT NULL,
    solicitante_perfil_id UUID NOT NULL,
    laboratorio_anterior_id UUID, laboratorio_propuesto_id UUID,
    docente_anterior_id UUID, docente_propuesto_id UUID,
    dia_anterior VARCHAR(15), dia_propuesto VARCHAR(15),
    hora_inicio_anterior TIME, hora_inicio_propuesta TIME,
    hora_fin_anterior TIME, hora_fin_propuesta TIME,
    resolucion TEXT,
    creada_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resuelta_en TIMESTAMPTZ,
    version INT8 NOT NULL DEFAULT 0
);
CREATE INDEX ix_solicitudes_cambio_plan ON solicitudes_cambio_planificacion(planificacion_id, creada_en DESC);

CREATE TABLE revisiones_solicitud_cambio (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    solicitud_id UUID NOT NULL REFERENCES solicitudes_cambio_planificacion(id),
    piso_id UUID NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    revisor_perfil_id UUID,
    observacion TEXT,
    creada_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resuelta_en TIMESTAMPTZ,
    CONSTRAINT uq_revision_solicitud_piso UNIQUE(solicitud_id, piso_id)
);

ALTER TABLE notificaciones_internas ADD COLUMN clave_evento VARCHAR(180);
CREATE UNIQUE INDEX uq_notificacion_clave_evento ON notificaciones_internas(clave_evento) WHERE clave_evento IS NOT NULL;
