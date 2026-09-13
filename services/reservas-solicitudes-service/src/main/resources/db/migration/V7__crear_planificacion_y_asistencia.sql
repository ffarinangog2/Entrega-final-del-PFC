CREATE TABLE planificaciones_semestre (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    periodo_id UUID NOT NULL,
    carrera_id UUID NOT NULL,
    materia_id UUID NOT NULL,
    docente_id UUID,
    laboratorio_id UUID NOT NULL,
    dia_semana VARCHAR(15) NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'BORRADOR',
    observacion TEXT,
    creado_por_perfil_id UUID NOT NULL,
    creada_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizada_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_planificacion_horas CHECK (hora_fin > hora_inicio),
    CONSTRAINT ck_planificacion_estado CHECK (estado IN ('BORRADOR','ENVIADA','PROPUESTA_CAMBIO','CONFIRMADA','RECHAZADA','CANCELADA'))
);
CREATE INDEX ix_planificacion_carrera_estado ON planificaciones_semestre(carrera_id, estado);
CREATE INDEX ix_planificacion_laboratorio_horario ON planificaciones_semestre(laboratorio_id, dia_semana, hora_inicio, hora_fin);

CREATE TABLE sesiones_asistencia (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reserva_id UUID NOT NULL,
    docente_id UUID NOT NULL,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    abierta_en TIMESTAMPTZ NOT NULL,
    expira_en TIMESTAMPTZ NOT NULL,
    cerrada_en TIMESTAMPTZ,
    estado VARCHAR(20) NOT NULL DEFAULT 'ABIERTA',
    CONSTRAINT ck_sesion_asistencia_estado CHECK (estado IN ('ABIERTA','CERRADA','VENCIDA')),
    CONSTRAINT ck_sesion_asistencia_ventana CHECK (expira_en > abierta_en)
);
CREATE INDEX ix_sesiones_asistencia_reserva ON sesiones_asistencia(reserva_id);

CREATE TABLE registros_asistencia (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sesion_id UUID NOT NULL REFERENCES sesiones_asistencia(id) ON DELETE CASCADE,
    estudiante_id UUID NOT NULL,
    registrada_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado VARCHAR(20) NOT NULL DEFAULT 'PRESENTE',
    CONSTRAINT uq_registro_asistencia_estudiante UNIQUE (sesion_id, estudiante_id),
    CONSTRAINT ck_registro_asistencia_estado CHECK (estado IN ('PRESENTE','JUSTIFICADA','AJUSTADA'))
);
CREATE INDEX ix_registros_asistencia_estudiante ON registros_asistencia(estudiante_id, registrada_en);
