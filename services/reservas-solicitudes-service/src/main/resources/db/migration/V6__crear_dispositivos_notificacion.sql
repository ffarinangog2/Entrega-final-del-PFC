CREATE TABLE dispositivos_notificacion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_auth_id UUID NOT NULL,
    perfil_id UUID NOT NULL,
    token VARCHAR(4096) NOT NULL,
    plataforma VARCHAR(20) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_dispositivos_notificacion_token UNIQUE (token),
    CONSTRAINT ck_dispositivos_plataforma CHECK (plataforma IN ('ANDROID'))
);
CREATE INDEX ix_dispositivos_notificacion_perfil ON dispositivos_notificacion (perfil_id, activo);
