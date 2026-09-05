CREATE TABLE notificaciones_internas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    perfil_id UUID NOT NULL,
    titulo VARCHAR(160) NOT NULL,
    cuerpo TEXT NOT NULL,
    tipo VARCHAR(60),
    referencia_id UUID,
    leida BOOL NOT NULL DEFAULT false,
    creada_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    leida_en TIMESTAMPTZ
);
CREATE INDEX ix_notificaciones_perfil_fecha ON notificaciones_internas (perfil_id, creada_en DESC);
