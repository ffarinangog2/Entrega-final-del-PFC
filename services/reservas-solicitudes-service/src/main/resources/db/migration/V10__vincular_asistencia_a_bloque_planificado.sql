ALTER TABLE sesiones_asistencia
    ALTER COLUMN reserva_id DROP NOT NULL,
    ADD COLUMN IF NOT EXISTS bloque_planificacion_id UUID,
    ADD COLUMN IF NOT EXISTS fecha_clase DATE;

ALTER TABLE sesiones_asistencia
    ADD CONSTRAINT IF NOT EXISTS fk_sesion_bloque_planificacion
        FOREIGN KEY (bloque_planificacion_id) REFERENCES planificaciones_semestre(id),
    ADD CONSTRAINT IF NOT EXISTS ck_sesion_origen
        CHECK (reserva_id IS NOT NULL OR bloque_planificacion_id IS NOT NULL);

CREATE UNIQUE INDEX IF NOT EXISTS uq_sesion_bloque_fecha
    ON sesiones_asistencia (bloque_planificacion_id, fecha_clase)
    WHERE bloque_planificacion_id IS NOT NULL;

