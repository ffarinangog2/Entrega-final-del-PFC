CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    creado_en TIMESTAMPTZ NOT NULL,
    expira_en TIMESTAMPTZ NOT NULL,
    usado_en TIMESTAMPTZ,
    solicitado_ip VARCHAR(64),
    CONSTRAINT fk_password_reset_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios_auth(id) ON DELETE CASCADE,
    CONSTRAINT ck_password_reset_fechas CHECK (expira_en > creado_en)
);
CREATE INDEX idx_password_reset_usuario ON password_reset_tokens (usuario_id);
CREATE INDEX idx_password_reset_expiracion ON password_reset_tokens (expira_en);
