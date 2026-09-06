ALTER TABLE sesiones_asistencia
    ADD COLUMN IF NOT EXISTS tema_actividad VARCHAR(500),
    ADD COLUMN IF NOT EXISTS observacion_uso TEXT,
    ADD COLUMN IF NOT EXISTS carrera_id_snapshot UUID,
    ADD COLUMN IF NOT EXISTS periodo_id_snapshot UUID,
    ADD COLUMN IF NOT EXISTS nivel_snapshot INTEGER,
    ADD COLUMN IF NOT EXISTS materia_id_snapshot UUID,
    ADD COLUMN IF NOT EXISTS docente_id_snapshot UUID,
    ADD COLUMN IF NOT EXISTS laboratorio_id_snapshot UUID,
    ADD COLUMN IF NOT EXISTS piso_id_snapshot UUID,
    ADD COLUMN IF NOT EXISTS dia_semana_snapshot VARCHAR(15),
    ADD COLUMN IF NOT EXISTS hora_inicio_snapshot TIME,
    ADD COLUMN IF NOT EXISTS hora_fin_snapshot TIME,
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE participantes_uso_laboratorio (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sesion_id UUID NOT NULL REFERENCES sesiones_asistencia(id),
    estudiante_perfil_id UUID NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    registrado_en TIMESTAMPTZ,
    registrado_por_perfil_id UUID,
    observacion TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_participante_uso_sesion_estudiante UNIQUE (sesion_id, estudiante_perfil_id),
    CONSTRAINT ck_participante_uso_estado CHECK (estado IN ('PENDIENTE', 'PRESENTE', 'AUSENTE'))
);

CREATE INDEX ix_participantes_uso_sesion_estado ON participantes_uso_laboratorio (sesion_id, estado);
CREATE INDEX ix_sesiones_uso_piso_fecha ON sesiones_asistencia (piso_id_snapshot, fecha_clase DESC)
    WHERE bloque_planificacion_id IS NOT NULL;
