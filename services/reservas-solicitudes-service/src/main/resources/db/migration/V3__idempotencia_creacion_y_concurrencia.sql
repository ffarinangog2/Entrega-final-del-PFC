CREATE TABLE idempotencia_creacion_solicitudes (
    clave VARCHAR(100) NOT NULL,
    operacion VARCHAR(50) NOT NULL,
    actor_id UUID NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    solicitud_id UUID NULL,
    creada_en TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    completada_en TIMESTAMPTZ NULL,
    CONSTRAINT pk_idempotencia_creacion_solicitudes PRIMARY KEY (clave),
    CONSTRAINT ck_idempotencia_creacion_operacion CHECK (operacion = 'CREAR_SOLICITUD'),
    CONSTRAINT ck_idempotencia_creacion_hash CHECK (length(payload_hash) = 64),
    CONSTRAINT fk_idempotencia_creacion_solicitud FOREIGN KEY (solicitud_id)
        REFERENCES solicitudes_reserva (id) ON DELETE RESTRICT,
    CONSTRAINT ck_idempotencia_creacion_resultado CHECK (
        (solicitud_id IS NULL AND completada_en IS NULL)
        OR (solicitud_id IS NOT NULL AND completada_en IS NOT NULL)
    )
);

CREATE UNIQUE INDEX uq_idempotencia_creacion_solicitud
    ON idempotencia_creacion_solicitudes (solicitud_id)
    WHERE solicitud_id IS NOT NULL;

CREATE INDEX ix_idempotencia_creacion_actor
    ON idempotencia_creacion_solicitudes (actor_id);

CREATE INDEX IF NOT EXISTS ix_reservas_disponibilidad
    ON reservas (laboratorio_id, fecha_reserva, hora_inicio, hora_fin)
    WHERE estado IN ('PROGRAMADA', 'EN_CURSO');
