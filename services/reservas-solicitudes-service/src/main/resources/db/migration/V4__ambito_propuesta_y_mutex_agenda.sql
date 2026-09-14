ALTER TABLE solicitudes_reserva ADD COLUMN piso_id UUID NULL;
ALTER TABLE reservas ADD COLUMN piso_id UUID NULL;

ALTER TABLE solicitudes_reserva ADD COLUMN propuesta_fecha DATE NULL;
ALTER TABLE solicitudes_reserva ADD COLUMN propuesta_hora_inicio TIME NULL;
ALTER TABLE solicitudes_reserva ADD COLUMN propuesta_hora_fin TIME NULL;
ALTER TABLE solicitudes_reserva ADD COLUMN propuesta_laboratorio_id UUID NULL;
ALTER TABLE solicitudes_reserva ADD COLUMN propuesta_observacion TEXT NULL;

ALTER TABLE solicitudes_reserva DROP CONSTRAINT ck_solicitudes_reserva_estado;
ALTER TABLE solicitudes_reserva ADD CONSTRAINT ck_solicitudes_reserva_estado
    CHECK (estado IN ('PENDIENTE', 'EN_REVISION', 'PROPUESTA', 'APROBADA',
                     'RECHAZADA', 'CANCELADA', 'EXPIRADA'));
ALTER TABLE historial_solicitudes DROP CONSTRAINT ck_historial_solicitudes_estado_anterior;
ALTER TABLE historial_solicitudes ADD CONSTRAINT ck_historial_solicitudes_estado_anterior
    CHECK (estado_anterior IS NULL OR estado_anterior IN ('PENDIENTE', 'EN_REVISION',
           'PROPUESTA', 'APROBADA', 'RECHAZADA', 'CANCELADA', 'EXPIRADA'));
ALTER TABLE historial_solicitudes DROP CONSTRAINT ck_historial_solicitudes_estado_nuevo;
ALTER TABLE historial_solicitudes ADD CONSTRAINT ck_historial_solicitudes_estado_nuevo
    CHECK (estado_nuevo IN ('PENDIENTE', 'EN_REVISION', 'PROPUESTA', 'APROBADA',
           'RECHAZADA', 'CANCELADA', 'EXPIRADA'));

CREATE INDEX ix_solicitudes_reserva_piso_estado ON solicitudes_reserva (piso_id, estado);
CREATE INDEX ix_reservas_piso_estado ON reservas (piso_id, estado);

CREATE TABLE mutex_agenda (
    laboratorio_id UUID NOT NULL,
    fecha DATE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_mutex_agenda PRIMARY KEY (laboratorio_id, fecha)
);
