CREATE TABLE idempotencia_aprobaciones (
    clave VARCHAR(100) NOT NULL,
    operacion VARCHAR(50) NOT NULL,
    solicitud_id UUID NOT NULL,
    reserva_id UUID,
    creada_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completada_en TIMESTAMPTZ,
    CONSTRAINT pk_idempotencia_aprobaciones PRIMARY KEY (clave),
    CONSTRAINT ck_idempotencia_aprobaciones_operacion
        CHECK (operacion = 'APROBAR_SOLICITUD'),
    CONSTRAINT fk_idempotencia_aprobaciones_solicitud
        FOREIGN KEY (solicitud_id) REFERENCES solicitudes_reserva (id) ON DELETE RESTRICT,
    CONSTRAINT fk_idempotencia_aprobaciones_reserva
        FOREIGN KEY (reserva_id) REFERENCES reservas (id) ON DELETE RESTRICT,
    CONSTRAINT ck_idempotencia_aprobaciones_resultado
        CHECK (
            (reserva_id IS NULL AND completada_en IS NULL)
            OR (reserva_id IS NOT NULL AND completada_en IS NOT NULL)
        )
);

CREATE UNIQUE INDEX uq_idempotencia_aprobaciones_reserva
    ON idempotencia_aprobaciones (reserva_id)
    WHERE reserva_id IS NOT NULL;

CREATE INDEX ix_idempotencia_aprobaciones_solicitud
    ON idempotencia_aprobaciones (solicitud_id);
