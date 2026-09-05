-- Amplía las columnas que pasarán a almacenar valores cifrados (AES-256-GCM)
-- y agrega un índice ciego (HMAC-SHA256) para permitir búsquedas exactas
-- sobre la identificación sin exponer el dato en texto plano.

ALTER TABLE perfiles
    ALTER COLUMN identificacion TYPE TEXT,
    ALTER COLUMN telefono TYPE TEXT,
    ALTER COLUMN direccion TYPE TEXT,
    ALTER COLUMN email_personal TYPE TEXT;

DROP INDEX IF EXISTS perfiles_identificacion_key CASCADE;

ALTER TABLE perfiles
    ADD COLUMN identificacion_hash VARCHAR(64);

-- Índice único parcial: solo exige unicidad donde ya exista hash.
-- Los registros sembrados por V2/V4 quedan sin hash hasta el backfill
-- (ver nota en el informe) y no rompen el arranque de la aplicación.
CREATE UNIQUE INDEX IF NOT EXISTS ux_perfiles_identificacion_hash
    ON perfiles (identificacion_hash)
    WHERE identificacion_hash IS NOT NULL;