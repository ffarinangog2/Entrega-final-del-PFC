ALTER TABLE password_reset_tokens
    ADD COLUMN invalidado_en TIMESTAMPTZ;

CREATE INDEX idx_password_reset_activos
    ON password_reset_tokens (usuario_id, expira_en)
    WHERE usado_en IS NULL AND invalidado_en IS NULL;
