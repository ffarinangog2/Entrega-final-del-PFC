-- Esquema ejecutable de reservas-solicitudes-service para CockroachDB E3.
-- Conserva el contrato definido por Flyway V1 y materializa ADR-003 y ADR-004.

CREATE DATABASE IF NOT EXISTS reservas_db;

USE reservas_db;

CREATE TABLE solicitudes_reserva (
    id UUID DEFAULT gen_random_uuid(),
    solicitante_id UUID NOT NULL,
    docente_id UUID NOT NULL,
    laboratorio_id UUID NOT NULL,
    materia_id UUID NOT NULL,
    periodo_lectivo_id UUID NOT NULL,
    fecha_reserva DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    numero_participantes INTEGER NOT NULL,
    motivo VARCHAR(500) NOT NULL,
    observacion TEXT,
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    clave_idempotencia VARCHAR(100),
    creada_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizada_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_solicitudes_reserva PRIMARY KEY (id),
    CONSTRAINT ck_solicitudes_reserva_horas
        CHECK (hora_fin > hora_inicio),
    CONSTRAINT ck_solicitudes_reserva_participantes_positivos
        CHECK (numero_participantes > 0),
    CONSTRAINT ck_solicitudes_reserva_motivo_no_vacio
        CHECK (btrim(motivo) <> ''),
    CONSTRAINT ck_solicitudes_reserva_estado
        CHECK (estado IN (
            'PENDIENTE',
            'EN_REVISION',
            'APROBADA',
            'RECHAZADA',
            'CANCELADA',
            'EXPIRADA'
        ))
);

COMMENT ON COLUMN solicitudes_reserva.solicitante_id IS
    'Referencia UUID externa sin clave foránea.';
COMMENT ON COLUMN solicitudes_reserva.docente_id IS
    'Referencia UUID externa sin clave foránea.';
COMMENT ON COLUMN solicitudes_reserva.laboratorio_id IS
    'Referencia UUID externa sin clave foránea.';
COMMENT ON COLUMN solicitudes_reserva.materia_id IS
    'Referencia UUID externa sin clave foránea.';
COMMENT ON COLUMN solicitudes_reserva.periodo_lectivo_id IS
    'Referencia UUID externa sin clave foránea.';

CREATE UNIQUE INDEX uq_solicitudes_reserva_clave_idempotencia
    ON solicitudes_reserva (clave_idempotencia)
    WHERE clave_idempotencia IS NOT NULL;

CREATE INDEX ix_solicitudes_reserva_solicitante_id
    ON solicitudes_reserva (solicitante_id);

CREATE INDEX ix_solicitudes_reserva_estado
    ON solicitudes_reserva (estado);

-- La fecha se antepone para permitir PARTITION BY RANGE y poda temporal.
CREATE INDEX ix_solicitudes_reserva_fecha_laboratorio_horario
    ON solicitudes_reserva (
        fecha_reserva,
        laboratorio_id,
        hora_inicio,
        hora_fin
    )
    PARTITION BY RANGE (fecha_reserva) (
        PARTITION p_pre_2026_q3 VALUES FROM (MINVALUE) TO ('2026-07-01'),
        PARTITION p_2026_q3 VALUES FROM ('2026-07-01') TO ('2026-10-01'),
        PARTITION p_2026_q4 VALUES FROM ('2026-10-01') TO ('2027-01-01'),
        PARTITION p_2027_q1 VALUES FROM ('2027-01-01') TO ('2027-04-01'),
        PARTITION p_2027_q2 VALUES FROM ('2027-04-01') TO ('2027-07-01'),
        PARTITION p_2027_q3 VALUES FROM ('2027-07-01') TO ('2027-10-01'),
        PARTITION p_2027_q4 VALUES FROM ('2027-10-01') TO ('2028-01-01'),
        PARTITION p_from_2028 VALUES FROM ('2028-01-01') TO (MAXVALUE)
    );

CREATE TABLE reservas (
    id UUID DEFAULT gen_random_uuid(),
    solicitud_id UUID NOT NULL,
    laboratorio_id UUID NOT NULL,
    responsable_id UUID NOT NULL,
    fecha_reserva DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'PROGRAMADA',
    codigo_reserva VARCHAR(50) NOT NULL,
    creada_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizada_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_reservas PRIMARY KEY (id),
    CONSTRAINT uq_reservas_solicitud_id UNIQUE (solicitud_id),
    CONSTRAINT uq_reservas_codigo_reserva UNIQUE (codigo_reserva),
    CONSTRAINT fk_reservas_solicitud
        FOREIGN KEY (solicitud_id)
        REFERENCES solicitudes_reserva (id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_reservas_horas
        CHECK (hora_fin > hora_inicio),
    CONSTRAINT ck_reservas_estado
        CHECK (estado IN (
            'PROGRAMADA',
            'EN_CURSO',
            'FINALIZADA',
            'CANCELADA',
            'NO_ASISTIDA'
        ))
);

COMMENT ON COLUMN reservas.solicitud_id IS
    'Relación interna con solicitudes_reserva; utiliza clave foránea local.';
COMMENT ON COLUMN reservas.laboratorio_id IS
    'Referencia UUID externa sin clave foránea.';
COMMENT ON COLUMN reservas.responsable_id IS
    'Referencia UUID externa sin clave foránea.';

-- Reutiliza exactamente los límites lógicos de solicitudes_reserva.
CREATE INDEX ix_reservas_fecha_laboratorio_horario
    ON reservas (
        fecha_reserva,
        laboratorio_id,
        hora_inicio,
        hora_fin
    )
    PARTITION BY RANGE (fecha_reserva) (
        PARTITION p_pre_2026_q3 VALUES FROM (MINVALUE) TO ('2026-07-01'),
        PARTITION p_2026_q3 VALUES FROM ('2026-07-01') TO ('2026-10-01'),
        PARTITION p_2026_q4 VALUES FROM ('2026-10-01') TO ('2027-01-01'),
        PARTITION p_2027_q1 VALUES FROM ('2027-01-01') TO ('2027-04-01'),
        PARTITION p_2027_q2 VALUES FROM ('2027-04-01') TO ('2027-07-01'),
        PARTITION p_2027_q3 VALUES FROM ('2027-07-01') TO ('2027-10-01'),
        PARTITION p_2027_q4 VALUES FROM ('2027-10-01') TO ('2028-01-01'),
        PARTITION p_from_2028 VALUES FROM ('2028-01-01') TO (MAXVALUE)
    );

CREATE INDEX ix_reservas_responsable_id
    ON reservas (responsable_id);

CREATE INDEX ix_reservas_estado
    ON reservas (estado);

CREATE TABLE historial_solicitudes (
    id UUID DEFAULT gen_random_uuid(),
    solicitud_id UUID NOT NULL,
    estado_anterior VARCHAR(30),
    estado_nuevo VARCHAR(30) NOT NULL,
    usuario_accion_id UUID NOT NULL,
    comentario TEXT,
    fecha_hora TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_historial_solicitudes PRIMARY KEY (id),
    CONSTRAINT fk_historial_solicitudes_solicitud
        FOREIGN KEY (solicitud_id)
        REFERENCES solicitudes_reserva (id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_historial_solicitudes_estado_anterior
        CHECK (
            estado_anterior IS NULL
            OR estado_anterior IN (
                'PENDIENTE',
                'EN_REVISION',
                'APROBADA',
                'RECHAZADA',
                'CANCELADA',
                'EXPIRADA'
            )
        ),
    CONSTRAINT ck_historial_solicitudes_estado_nuevo
        CHECK (estado_nuevo IN (
            'PENDIENTE',
            'EN_REVISION',
            'APROBADA',
            'RECHAZADA',
            'CANCELADA',
            'EXPIRADA'
        )),
    CONSTRAINT ck_historial_solicitudes_estados_distintos
        CHECK (
            estado_anterior IS NULL
            OR estado_anterior <> estado_nuevo
        )
);

COMMENT ON COLUMN historial_solicitudes.solicitud_id IS
    'Relación interna con solicitudes_reserva; utiliza clave foránea local.';
COMMENT ON COLUMN historial_solicitudes.usuario_accion_id IS
    'Referencia UUID externa sin clave foránea.';

CREATE INDEX ix_historial_solicitudes_fecha_solicitud
    ON historial_solicitudes (fecha_hora, solicitud_id)
    PARTITION BY RANGE (fecha_hora) (
        PARTITION p_pre_2026_q3
            VALUES FROM (MINVALUE) TO ('2026-07-01 00:00:00+00'),
        PARTITION p_2026_q3
            VALUES FROM ('2026-07-01 00:00:00+00') TO ('2026-10-01 00:00:00+00'),
        PARTITION p_2026_q4
            VALUES FROM ('2026-10-01 00:00:00+00') TO ('2027-01-01 00:00:00+00'),
        PARTITION p_2027_q1
            VALUES FROM ('2027-01-01 00:00:00+00') TO ('2027-04-01 00:00:00+00'),
        PARTITION p_2027_q2
            VALUES FROM ('2027-04-01 00:00:00+00') TO ('2027-07-01 00:00:00+00'),
        PARTITION p_2027_q3
            VALUES FROM ('2027-07-01 00:00:00+00') TO ('2027-10-01 00:00:00+00'),
        PARTITION p_2027_q4
            VALUES FROM ('2027-10-01 00:00:00+00') TO ('2028-01-01 00:00:00+00'),
        PARTITION p_from_2028
            VALUES FROM ('2028-01-01 00:00:00+00') TO (MAXVALUE)
    );

CREATE TABLE bloqueos_agenda (
    id UUID DEFAULT gen_random_uuid(),
    laboratorio_id UUID NOT NULL,
    fecha DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    motivo VARCHAR(500) NOT NULL,
    creado_por UUID NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_bloqueos_agenda PRIMARY KEY (id),
    CONSTRAINT ck_bloqueos_agenda_horas
        CHECK (hora_fin > hora_inicio),
    CONSTRAINT ck_bloqueos_agenda_motivo_no_vacio
        CHECK (btrim(motivo) <> '')
);

COMMENT ON COLUMN bloqueos_agenda.laboratorio_id IS
    'Referencia UUID externa sin clave foránea.';
COMMENT ON COLUMN bloqueos_agenda.creado_por IS
    'Referencia UUID externa sin clave foránea.';

-- Reutiliza los límites de calendario de solicitudes y reservas.
CREATE INDEX ix_bloqueos_agenda_fecha_laboratorio_horario
    ON bloqueos_agenda (
        fecha,
        laboratorio_id,
        hora_inicio,
        hora_fin,
        activo
    )
    PARTITION BY RANGE (fecha) (
        PARTITION p_pre_2026_q3 VALUES FROM (MINVALUE) TO ('2026-07-01'),
        PARTITION p_2026_q3 VALUES FROM ('2026-07-01') TO ('2026-10-01'),
        PARTITION p_2026_q4 VALUES FROM ('2026-10-01') TO ('2027-01-01'),
        PARTITION p_2027_q1 VALUES FROM ('2027-01-01') TO ('2027-04-01'),
        PARTITION p_2027_q2 VALUES FROM ('2027-04-01') TO ('2027-07-01'),
        PARTITION p_2027_q3 VALUES FROM ('2027-07-01') TO ('2027-10-01'),
        PARTITION p_2027_q4 VALUES FROM ('2027-10-01') TO ('2028-01-01'),
        PARTITION p_from_2028 VALUES FROM ('2028-01-01') TO (MAXVALUE)
    );

CREATE TABLE configuraciones_reserva (
    id UUID DEFAULT gen_random_uuid(),
    anticipacion_minima_horas INTEGER NOT NULL,
    anticipacion_maxima_dias INTEGER NOT NULL,
    duracion_minima_minutos INTEGER NOT NULL,
    duracion_maxima_minutos INTEGER NOT NULL,
    permite_fines_semana BOOLEAN NOT NULL DEFAULT FALSE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creada_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizada_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_configuraciones_reserva PRIMARY KEY (id),
    CONSTRAINT ck_configuraciones_reserva_valores_no_negativos
        CHECK (
            anticipacion_minima_horas >= 0
            AND anticipacion_maxima_dias >= 0
            AND duracion_minima_minutos >= 0
            AND duracion_maxima_minutos >= 0
        ),
    CONSTRAINT ck_configuraciones_reserva_duracion_valida
        CHECK (duracion_maxima_minutos >= duracion_minima_minutos)
);

CREATE UNIQUE INDEX uq_configuraciones_reserva_unica_activa
    ON configuraciones_reserva (activo)
    WHERE activo;

CREATE FUNCTION actualizar_actualizada_en()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.actualizada_en = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_solicitudes_reserva_actualizada_en
    BEFORE UPDATE ON solicitudes_reserva
    FOR EACH ROW
    EXECUTE FUNCTION actualizar_actualizada_en();

CREATE TRIGGER trg_reservas_actualizada_en
    BEFORE UPDATE ON reservas
    FOR EACH ROW
    EXECUTE FUNCTION actualizar_actualizada_en();

CREATE TRIGGER trg_configuraciones_reserva_actualizada_en
    BEFORE UPDATE ON configuraciones_reserva
    FOR EACH ROW
    EXECUTE FUNCTION actualizar_actualizada_en();

-- Política predeterminada de replicación acordada en ADR-004.
ALTER RANGE default CONFIGURE ZONE USING num_replicas = 3;
